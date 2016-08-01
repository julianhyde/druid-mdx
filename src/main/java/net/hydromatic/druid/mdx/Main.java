/*
 * Licensed to Julian Hyde under one or more contributor license
 * agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership. Julian Hyde
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hydromatic.druid.mdx;

import org.olap4j.CellSet;
import org.olap4j.OlapConnection;
import org.olap4j.OlapStatement;
import org.olap4j.layout.CellSetFormatter;
import org.olap4j.layout.RectangularCellSetFormatter;
import org.olap4j.metadata.Cube;

import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Example that runs an MDX query against Druid.
 *
 * <p>First, it creates a JDBC connection using Apache Calcite's Druid adapter.
 * Next, it generates a Mondrian model (assuming that numeric columns are
 * measures and other columns are dimensions).
 * Last, it connects to Mondrian and executes an MDX query.
 *
 * <p>Because the Mondrian model is generated on the fly, you could probably
 * apply this example to other Druid data sources. If you want advanced features
 * such as hierarchies, dimensions with composite keys, or calculated members,
 * you can write the Mondrian model by hand.
 */
public class Main {
  private static final String CALCITE_MODEL =  ""
      + "{\n"
      + "  \"version\": \"1.0\",\n"
      + "  \"defaultSchema\": \"wiki\",\n"
      + "  \"schemas\": [\n"
      + "    {\n"
      + "      \"type\": \"custom\",\n"
      + "      \"name\": \"wiki\",\n"
      + "      \"factory\": \"org.apache.calcite.adapter.druid.DruidSchemaFactory\",\n"
      + "      \"operand\": {\n"
      + "        \"url\": \"http://localhost:8082\",\n"
      + "        \"coordinatorUrl\": \"http://localhost:8081\"\n"
      + "      }\n"
      + "    }\n"
      + "  ]\n"
      + "}\n"
      + "";

  public static void main(String[] args) {
    final PrintWriter w = new PrintWriter(System.out);
    final String url = "jdbc:calcite:model='inline:" + CALCITE_MODEL + "'";
    final String mondrianModel = getMondrianModel(url, "wiki", "wikiticker", w);
    w.println("Generated Mondrian model:");
    w.println(mondrianModel);
    w.flush();

    final String url2 = "jdbc:mondrian:CatalogContent=\"" + mondrianModel + "\""
        + ";Jdbc=" + url;
    try (Connection c = DriverManager.getConnection(url2)) {
      final OlapConnection c2 = c.unwrap(OlapConnection.class);
      w.println("Discovering cubes:");
      for (Cube cube : c2.getOlapSchema().getCubes()) {
        w.println(cube.getName());
      }
      w.println();

      final String mdx = "select\n"
          + " [Measures].members on columns,\n"
          + " Order([countryName].members, [Measures].[added], DESC) on rows\n"
          + "from [wiki]";
      try (final OlapStatement s = c2.createStatement();
           final CellSet cellSet = s.executeOlapQuery(mdx)) {
        CellSetFormatter f = new RectangularCellSetFormatter(true);
        f.format(cellSet, w);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    w.flush();
  }

  /** Generates a Mondrian model based on a single table. */
  private static String getMondrianModel(String url, String schema,
      String table, PrintWriter w) {
    final List<String> dimensionColumns = new ArrayList<>();
    final List<String> measureColumns = new ArrayList<>();
    try (Connection c = DriverManager.getConnection(url);
         ResultSet r = c.getMetaData().getColumns(null, schema, table, null)) {
      while (r.next()) {
        final String columnName = r.getString("COLUMN_NAME");
        final int type = r.getInt("DATA_TYPE");
        System.out.println(columnName + ":" + type);
        switch (type) {
        case Types.INTEGER:
        case Types.BIGINT:
          measureColumns.add(columnName);
          break;
        default:
          dimensionColumns.add(columnName);
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    w.println("Discovering columns:");
    w.println("dimensions=" + dimensionColumns);
    w.println("measures=" + measureColumns);
    w.println();

    final StringBuilder dimensions = new StringBuilder();
    final StringBuilder links = new StringBuilder();
    for (String c : dimensionColumns) {
      dimensions.append("      <Dimension name='" + c + "'>\n"
          + "        <Attributes>\n"
          + "          <Attribute name='" + c + "' table='" + table
          + "' keyColumn='" + c + "'/>\n"
          + "        </Attributes>\n"
          + "      </Dimension>\n");
      links.append("          <FactLink dimension='" + c + "'/>\n");
    }
    final StringBuilder measures = new StringBuilder();
    for (String c : measureColumns) {
      measures.append("          <Measure name='" + c
          + "' aggregator='sum' column='" + c + "'/>\n");
    }
    return ""
        + "<?xml version='1.0'?>\n"
        + "<Schema name='wiki' metamodelVersion='4.0'>\n"
        + "  <PhysicalSchema>\n"
        + "      <Table name='wikiticker'/>\n"
        + "  </PhysicalSchema>\n"
        + "  <Cube name='wiki'>\n"
        + "    <Dimensions>\n"
        + dimensions
        + "    </Dimensions>\n"
        + "    <MeasureGroups>\n"
        + "      <MeasureGroup table='wikiticker'>\n"
        + "        <Measures>\n"
        + measures
        + "        </Measures>\n"
        + "        <DimensionLinks>\n"
        + links
        + "        </DimensionLinks>\n"
        + "      </MeasureGroup>\n"
        + "    </MeasureGroups>\n"
        + "  </Cube>\n"
        + "</Schema>\n";
  }
}

// End Main.java

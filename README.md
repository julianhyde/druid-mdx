# druid-mdx

This project is an example of running MDX on
[Druid](http://druid.io) via
[Mondrian](http://mondrian.pentaho.org) and
[Apache Calcite](http://calcite.apache.org).

## What it does

First, it creates a JDBC connection using
[Calcite's Druid adapter](https://calcite.apache.org/docs/druid_adapter.html).
Next, it generates a Mondrian model (assuming that numeric columns are
measures and other columns are dimensions).
Last, it connects to Mondrian and executes an MDX query.

## Running it

Here is the Mondrian model that is generated:

```xml
<?xml version='1.0'?>
<Schema name='wiki' metamodelVersion='4.0'>
  <PhysicalSchema>
      <Table name='wikiticker'/>
  </PhysicalSchema>
  <Cube name='wiki'>
    <Dimensions>
      <Dimension name='channel'>
        <Attributes>
          <Attribute name='channel' table='wikiticker' keyColumn='channel'/>
        </Attributes>
      </Dimension>
      <Dimension name='cityName'>
        <Attributes>
          <Attribute name='cityName' table='wikiticker' keyColumn='cityName'/>
        </Attributes>
      </Dimension>
      <Dimension name='comment'>
        <Attributes>
          <Attribute name='comment' table='wikiticker' keyColumn='comment'/>
        </Attributes>
      </Dimension>
      <Dimension name='countryIsoCode'>
        <Attributes>
          <Attribute name='countryIsoCode' table='wikiticker' keyColumn='countryIsoCode'/>
        </Attributes>
      </Dimension>
      <Dimension name='countryName'>
        <Attributes>
          <Attribute name='countryName' table='wikiticker' keyColumn='countryName'/>
        </Attributes>
      </Dimension>
      <Dimension name='isAnonymous'>
        <Attributes>
          <Attribute name='isAnonymous' table='wikiticker' keyColumn='isAnonymous'/>
        </Attributes>
      </Dimension>
      <Dimension name='isMinor'>
        <Attributes>
          <Attribute name='isMinor' table='wikiticker' keyColumn='isMinor'/>
        </Attributes>
      </Dimension>
      <Dimension name='isNew'>
        <Attributes>
          <Attribute name='isNew' table='wikiticker' keyColumn='isNew'/>
        </Attributes>
      </Dimension>
      <Dimension name='isRobot'>
        <Attributes>
          <Attribute name='isRobot' table='wikiticker' keyColumn='isRobot'/>
        </Attributes>
      </Dimension>
      <Dimension name='isUnpatrolled'>
        <Attributes>
          <Attribute name='isUnpatrolled' table='wikiticker' keyColumn='isUnpatrolled'/>
        </Attributes>
      </Dimension>
      <Dimension name='metroCode'>
        <Attributes>
          <Attribute name='metroCode' table='wikiticker' keyColumn='metroCode'/>
        </Attributes>
      </Dimension>
      <Dimension name='namespace'>
        <Attributes>
          <Attribute name='namespace' table='wikiticker' keyColumn='namespace'/>
        </Attributes>
      </Dimension>
      <Dimension name='page'>
        <Attributes>
          <Attribute name='page' table='wikiticker' keyColumn='page'/>
        </Attributes>
      </Dimension>
      <Dimension name='regionIsoCode'>
        <Attributes>
          <Attribute name='regionIsoCode' table='wikiticker' keyColumn='regionIsoCode'/>
        </Attributes>
      </Dimension>
      <Dimension name='regionName'>
        <Attributes>
          <Attribute name='regionName' table='wikiticker' keyColumn='regionName'/>
        </Attributes>
      </Dimension>
      <Dimension name='user'>
        <Attributes>
          <Attribute name='user' table='wikiticker' keyColumn='user'/>
        </Attributes>
      </Dimension>
      <Dimension name='user_unique'>
        <Attributes>
          <Attribute name='user_unique' table='wikiticker' keyColumn='user_unique'/>
        </Attributes>
      </Dimension>
    </Dimensions>
    <MeasureGroups>
      <MeasureGroup table='wikiticker'>
        <Measures>
          <Measure name='__time' aggregator='sum' column='__time'/>
          <Measure name='added' aggregator='sum' column='added'/>
          <Measure name='count' aggregator='sum' column='count'/>
          <Measure name='deleted' aggregator='sum' column='deleted'/>
          <Measure name='delta' aggregator='sum' column='delta'/>
        </Measures>
        <DimensionLinks>
          <FactLink dimension='channel'/>
          <FactLink dimension='cityName'/>
          <FactLink dimension='comment'/>
          <FactLink dimension='countryIsoCode'/>
          <FactLink dimension='countryName'/>
          <FactLink dimension='isAnonymous'/>
          <FactLink dimension='isMinor'/>
          <FactLink dimension='isNew'/>
          <FactLink dimension='isRobot'/>
          <FactLink dimension='isUnpatrolled'/>
          <FactLink dimension='metroCode'/>
          <FactLink dimension='namespace'/>
          <FactLink dimension='page'/>
          <FactLink dimension='regionIsoCode'/>
          <FactLink dimension='regionName'/>
          <FactLink dimension='user'/>
          <FactLink dimension='user_unique'/>
        </DimensionLinks>
      </MeasureGroup>
    </MeasureGroups>
  </Cube>
</Schema>
```

When the query
 
```sql
select
  [Measures].members on columns,
  Order([countryName].members, [Measures].[added], DESC) on rows
from [wiki]
```

is run, it produces the following output:

```
                                            __time                 added     count  deleted delta
=============== =========================== ====================== ========= ====== ======= =========
All countryName                             56,592,352,129,829,184 9,385,573 39,244 394,298 8,991,275
                #null                       51,113,948,353,410,816 8,761,516 35,445 346,816 8,414,700
                Colombia                        99,503,304,065,741    60,398     69     787    59,611
                Russia                         279,761,176,573,129    50,561    194   2,457    48,104
                United States                  761,411,157,953,310    44,433    528   5,551    38,882
                Italy                          369,169,251,122,223    41,073    256   1,982    39,091
                France                         295,623,947,063,302    39,853    205   2,572    37,281
                United Kingdom                 337,444,146,319,684    38,587    234   2,730    35,857
                India                          180,257,135,637,316    30,313    125   1,147    29,166
                Germany                        233,615,258,310,628    26,807    162   1,224    25,583
```

## How to build and run

To build, you need Java 1.7 or 1.8, Apache Maven 3.2.1 or higher:

```sh
$ mvn install
```

To run, you need Druid running with the query node at
`http://localhost:8082`, the coordinator at `http://localhost:8081`,
populated with the the "wikiticker" data source.

You can run from the command line as follows:

```sh
$ mvn exec:java
```

or run `net.hydromatic.druid.mdx.Main` from any Java IDE.

## Running against different schemas

Because the Mondrian model is generated on the fly, you could probably
apply this example to other Druid data sources. If you want advanced
features such as hierarchies, dimensions with composite keys, or
calculated members, you can write the Mondrian model by hand.


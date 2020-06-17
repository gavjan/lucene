# Search Indexing with Apache Lucene

Using Search Indexing for efficient searching inside popular file types (.pdf .rtf ...) that are located under a directory that has already been indexed

## Task Description

Full task descrption (in Polish) can be found in `Lucene.pdf`

## Build

Indexer: with "argument"
```
mvn compile exec:java -Dexec.mainClass=pl.edu.mimuw.gc401929.core.CustomIndexer -Dexec.args="{argument}"
```

Searcher:
```
mvn compile exec:java -Dexec.mainClass=pl.edu.mimuw.gc401929.core.JLineClass
```

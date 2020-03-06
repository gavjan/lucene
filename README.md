## Lucene

Indexed searching in documents. Allows indexing the content of popular documents formats (including PDF, not just pure text files). When the document crawler collection is large and consists of several hundred or even several thousand files - scanning the content of all documents each time for a given job will be slow. Hence the need to use an index that significantly speeds up the search process.

## Build

Indexer: with "argument"
```
mvn compile exec:java -Dexec.mainClass=pl.edu.mimuw.gc401929.core.CustomIndexer -Dexec.args="{argument}"
```

Searcher:
```
mvn compile exec:java -Dexec.mainClass=pl.edu.mimuw.gc401929.core.JLineClass
```

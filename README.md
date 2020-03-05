## Lucene

The purpose of the tasks is to create applications for searching text in documents Tools developed as part of this task allow indexing the content of popular documents formats (including PDF, not just pure text files). 
In addition, text search must is fast (thanks to indexing), also when the document crawler collection is large and 
consists of several hundred or even several thousand files - scanning  the content of all 
documents each time for a given job will be insufficient here. Hence the need to use 
an index that significantly speeds up the search process.

## Build

Indexer: with "argument"
```
mvn compile exec:java -Dexec.mainClass=pl.edu.mimuw.gc401929.core.CustomIndexer -Dexec.args="{argument}"
```

Searcher:
```
mvn compile exec:java -Dexec.mainClass=pl.edu.mimuw.gc401929.core.JLineClass
```

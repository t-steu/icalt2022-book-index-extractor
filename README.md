# How to Use

Pdf-index-extract is a fast and simple tool to extract back-of-the-book indices from PDF textbooks.

`pdf-index-extract <pdf-input-path> <index-xml-output-path>`

Yields an output file of the form:

    <index>
      <file>input-filename</file>
      <entries>
        <entry>
          <phrase>adhesive force</phrase>
            <pagenumbers>
              <number>584</number>
            </pagenumbers>
        </entry>
        ...
      </entires>
    </index>


## Evaluation Results

We evaluated the tool on a collection of books crawled from SpringerLink
against the corresponding index extracted from the related EPUB file.

The ISBNs of the books considered for eval (if they had an epub at time of the study) can be found `evaluation-isbns.csv`.

| Avg-Precision  | Avg-Recall | Avg-Macro-F1 | 
| -----| -------- | ------ | 
| 0.91  | 0.93  | 0.92  |


## Used Evaluation Files

We downloaded all German books specified in the `evaluation_data` folder for index evaluation.
The English books were used for the other analyses in the paper (if they contained an index).
For index evaluation, we only considered books were the PDF and the EPUB contained an index.
Therefore, the number of books in the files differs slightly from the number reported in the paper.


## How to built

1. clone the repository
2. run  `mvn install`
3. The command line tool can be found in `<working-dir>/bin/pdf-index-extract`


## How to Cite

We submitted a paper to IEEE ICALT describing the index extractor and its use for automatic corpus construction.

> T. Steuer, A. Filighera, N. Mouhammad, G. Zimmer and T. Tregel, "Learning-Relevant Concept Extraction By Utilizing Automatically Generated Textbook Corpora," International Conference on Advanced Learning Technologies (ICALT), 2022, pp. 379-383, doi: 10.1109/ICALT55010.2022.00117.

```
    @INPROCEEDINGS{steuer-icalt-2022,
        author={Steuer, Tim and Filighera, Anna and Mouhammad, Nina and Zimmer, Gianluca and Tregel, Thomas},
        booktitle={International Conference on Advanced Learning Technologies (ICALT)}, 
        title={Learning-Relevant Concept Extraction By Utilizing Automatically Generated Textbook Corpora}, 
        year={2022},
        volume={},
        number={},
        pages={379-383},
        doi={10.1109/ICALT55010.2022.00117}
   }
```


## Acknowledgements

The tool was built en-top of the great [PDFAct](https://github.com/ad-freiburg/pdfact) by Hannah Bast / Claudius Korzen







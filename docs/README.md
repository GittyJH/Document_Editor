# Document Editor

```sh
mvn clean test
mvn package
```

Run the command interface:

```sh
java -jar target/document-editor-1.0.0.jar
```

Run a command script:

```sh
java -jar target/document-editor-1.0.0.jar path/to/commands.txt
```

A script contains one complete command per line. Blank lines and lines whose first non-whitespace character is `#` are ignored. The script runner stops at the first failed command and reports its line number. The interactive and scripted interfaces use the same `CommandInterface` implementation.

## Command reference

Arguments containing spaces should be enclosed in double quotes. Backslash escapes the next character inside or outside quoted arguments.

| Area                   | Commands                                                                                                                                                                                |
|------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Document metadata      | `document create "Report"`; `document title "Revised Report"`                                                                                                                           |
| Structure              | `section add root "Introduction"`; `section add 0 "Background"`; `section remove 0.0`; `paragraph add 0.0 "Nested text"`                                                                |
| Text edits             | `insert 0 5 " text"`; `delete 0 5 10`; `edit-replace 0 0 4 "new"`; `move 0 0 4 12`; `split 0 8`; `merge 0`                                                                              |
| Formatting             | `format 0 0 5 bold true`; `format 0 0 5 font "Courier New"`; `format 0 0 5 size 14`; `format 0 0 5 color blue`; `format-title 0 bold true`; use `inherit` to clear an explicit override |
| Styles                 | `style define report font "Courier New"`; `style apply 0 report`                                                                                                                        |
| Search and replace     | `find substring "term"`; `find whole_word "term"`; `replace-first substring "old" "new"`; `replace-all whole_word "old" "new"`                                                          |
| History                | `undo`; `redo`; `checkpoint create before-edit`; `checkpoint restore before-edit`                                                                                                       |
| Rendering and status   | `render plain 72 greedy`; `render markup 72 hyphenation`; `stats`                                                                                                                       |
| Macros                 | `macro start name`; run commands; `macro stop`; `macro replay name`; `macro list`                                                                                                       |
| History file extension | `history export history.txt`; `history replay history.txt`                                                                                                                              |
| Help                   | `help`                                                                                                                                                                                  |

### Indexing and operation semantics

- Paragraph indices are zero-based and use depth-first document order across all nested sections.
- Section paths are zero-based dotted paths. `0.2.1` denotes the second nested section beneath the third nested section beneath root section zero. The literal `root` is used only when adding a root section.
- Edit ranges use half-open offsets `[start,end)`. Insert accepts offsets from zero through the paragraph length. Empty ranges are valid for insertions and no-op deletions; invalid bounds are rejected before state changes.
- Move source and destination offsets are interpreted in the original paragraph. A destination inside the moved range is rejected. A destination after the source is adjusted after removal.
- `merge N` joins paragraph N with the immediately following paragraph only when both paragraphs are siblings in the same section.
- Search and replacement traverse paragraphs throughout the nested document. `whole_word` uses ASCII letters, digits, and underscore as word characters.
- Statistics traverse current document state on request. Word and character counts include section titles as well as paragraph text; paragraph count counts paragraph nodes only.
- The greedy strategy packs complete words per line. The hyphenation strategy is the alternative strategy and may split an over-width word, ensuring a visibly different result for suitable input. Both preserve the relationship between rendered character ranges and formatting spans.
- A macro replay is one atomic undoable command. It may contain edits, formatting, replace operations, and read-only `render`, `stats`, and `find` actions. If a child edit fails, the macro's document and style state are restored.
- History replay always starts from an empty target document. The history log is a versioned, line-oriented UTF-8 format with explicit command descriptors and EXECUTE/UNDO/REDO events; replay reconstructs the undo/redo stacks as well as the document, and does not use Java object serialization.
- Inherited formatting is resolved at render time: document defaults, containing-section styles from outermost to innermost, then explicit section-element properties, then paragraph range formatting. Clearing an explicit property with `inherit` reveals the applicable inherited value. Updating a named style changes all inheriting content dynamically.

## Architecture

The implementation is arranged in layers and packages:

- `model`: `Document`, `Section`, `Paragraph`, `SectionElement`, formatted text runs, formatting, matches, snapshots, and change listeners. `Section` is both a composite and a child `SectionElement`; `Paragraph` is a leaf.
- `command`: parser, command interface, edit/format/structure/style/replace/state-restore commands, and the command processor. Commands encapsulate mutation and undo/redo behavior; command parsing is the only component that interprets the text grammar.
- `history`: command stacks, named snapshots, a filesystem abstraction, explicit portable history codec, editor-state codec, and replay engine.
- `macro`: named recordings and macro command aggregation.
- `render`: separate plain-text and marked-up views plus `LineBreakStrategy`, `GreedyLineBreak`, and `HyphenationLineBreak`.
- `service`: independent nested-document search, statistics, and inherited-style resolution.
- `actor`: `User`, `Author`, and `Reviewer` role objects sharing the same command interface.

### Pattern use

- **Composite:** nested `Section` objects contain paragraphs and further sections through `SectionElement`; traversal code does not assume fixed depth.
- **Command:** each document mutation implements `Command`. The history manager operates against the interface, so new command types do not require changing undo/redo. Replace-all and macro replay each form a single parent command.
- **Strategy:** `Renderer` accepts a `LineBreakStrategy`; line-breaking algorithms can be added independently of the model and render views.

The renderer uses a small `DocumentRenderView` abstraction for plain vs. marked-up output. `DocumentChangeListener` provides an observer seam for future reactions without coupling edits to status reporting or other listeners.

## Tests and traceability

Tests are organized by behavior, not by parser implementation detail. They cover the core UC-01–UC-08 use cases and both extensions UC-09 and UC-10. See [docs/TRACEABILITY.md](TRACEABILITY.md) for the mapping to the requirements and test classes.

The optional Section 6.5.6 storage-sharing target (one hundred thousand characters stored using five distinct formatting values) is not claimed as implemented. Formatting is stored as compact, coalesced ranges rather than a formatting object per character.
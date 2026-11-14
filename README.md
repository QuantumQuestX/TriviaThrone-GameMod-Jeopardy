# TriviaThrone Game Mod Template

This repository is a starting point for creating a game mod for the
[TriviaThrone](https://github.com/QuantumQuestX/TriviaThrone) server. It implements a complete,
single-question quiz named `example-quiz`, including the game-package creator, server-side game
session, editor page, and gameplay page.

## Prerequisites

- Java 25
- Maven 3.9 or later
- A local checkout of the [TriviaThrone server](https://github.com/QuantumQuestX/TriviaThrone)

The game API is currently consumed from the local Maven repository. From the TriviaThrone source
tree, install it before building this template:

```sh
./mvnw -pl triviathrone-api/game-api -am install -DskipTests
```

## Build and install

Build the mod from this directory:

```sh
mvn clean verify
```

Copy the resulting JAR to the server's game-mod directory, then restart the server:

```sh
cp target/example-game-mod-1.0.0-SNAPSHOT.jar /path/to/TriviaThrone/mods/games/
```

By default, the server loads every JAR in `mods/games/`. Set
`triviathrone.mods.games-directory` to use another directory. The server rejects duplicate mod IDs
and game types, so assign unique values before installing a renamed copy of this template.

## What the example does

The example appears in the game creator as **Example quiz**. The editor collects a title, question,
and answer, then creates a `.game.throne` package containing `game.properties`. When the package is
uploaded and a lobby starts it, the custom game page displays the question; the host can reveal the
answer.

| Area              | Template implementation                                  |
|-------------------|----------------------------------------------------------|
| Game type         | `example-quiz`                                           |
| Package extension | `.game.throne`                                           |
| Package data      | `game.properties` with `title`, `question`, and `answer` |
| Editor UI         | `src/main/resources/ui/editor.html`                      |
| Game UI           | `src/main/resources/ui/game.html`                        |

## Customise the template

1. Change the Maven coordinates and artefact name in `pom.xml`.
2. Replace `example.triviathrone` and `ExampleQuiz` throughout the Java source and resource paths.
3. Give the mod a unique `id`, name, description, version, and provider class in
   `src/main/resources/mod.xml`. Keep its `apiVersion` equal to
   `triviathrone.api.version` in `pom.xml`.
4. Update the provider's `gameType()`, supported package extension, data-file name, loading logic,
   creator, and `GameUiDescriptor`.
5. Update the game definition and session to represent your game rules and state. A session must
   validate game actions and return the current and final `IGameMessage` values.
6. Update the creator schema and output so it writes the data your provider loads. Close every
   stream obtained from `GameFileWriter`.
7. Update `ui/editor.html`, `ui/game.html`, and their scripts to match the creator data and game
   messages.

## Mod discovery contract

TriviaThrone discovers each game mod through both files below. They must name the same set of
provider classes:

```text
src/main/resources/mod.xml
src/main/resources/META-INF/services/quantumquest.triviathrone.api.game.IGameProvider
```

`mod.xml` is packaged at the root of the JAR. Its `<providers>` entries must exactly match the
newline-separated provider classes in the `META-INF/services` file; otherwise the server refuses to
load the JAR. UI pages declared with `GameUiDescriptor` are relative HTML paths below the JAR's
`ui/` resource directory.

The API JAR is supplied by the server, so `triviathrone-game-api` must remain a `provided`
dependency. Jackson is also supplied by the server in this template; package any additional runtime
dependencies required by your mod. When the server API changes, update both
`triviathrone.api.version` and `mod.xml`'s `apiVersion` to compatible versions.

## License

This template is distributed under the [MIT License](LICENSE).

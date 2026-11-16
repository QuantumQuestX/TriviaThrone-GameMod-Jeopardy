# TriviaThrone Jeopardy Game Mod

Jeopardy is a game mod for the [TriviaThrone](https://github.com/QuantumQuestX/TriviaThrone)
server. It adds the `jeopardy` game type, a Jeopardy board with rounds, categories, selectable
questions, optional multiple-choice answers, and optional media resources.

## Requirements

- Java 25
- Maven 3.9 or later
- TriviaThrone game API 2.0.0 through 2.x releases

The game API is provided by the TriviaThrone server and is not packaged into this mod. If the API
is not already installed locally, install it from the TriviaThrone source tree:

```sh
./mvnw -pl triviathrone-api/game-api -am install -DskipTests
```

## Build

Run from the repository root:

```sh
mvn clean verify
```

The resulting mod is:

```text
target/jeopardy-1.0.0-SNAPSHOT.jar
```

Copy the JAR into the server's configured `mods/games/` directory and restart TriviaThrone.

## mod identity

| Property          | Value                                                     |
|-------------------|-----------------------------------------------------------|
| Mod ID            | `jeopardy`                                                |
| Game type         | `jeopardy`                                                |
| Package extension | `.game.throne`                                            |
| Provider          | `quantumquest.triviathrone.jeopardy.JeopardyGameProvider` |
| Game-data format  | `1.0`                                                     |
| API version       | `2.1.0`                                                   |

The provider is registered in both `src/main/resources/mod.xml` and
`src/main/resources/META-INF/services/quantumquest.triviathrone.api.game.IGameProvider`. Keep
these registrations in sync when changing the provider class.

## Game package format

A Jeopardy package contains `manifest.xml`, `game.xml`, and any files referenced by its resources.
The manifest must identify this provider:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<gamePackage manifestVersion="2.0">
  <mod id="jeopardy" version="1.0.0" apiVersion="2.1.0"/>
  <game
      type="jeopardy"
      providerClass="quantumquest.triviathrone.jeopardy.JeopardyGameProvider"
      dataFile="game.xml"
      formatVersion="1.0"/>
  <metadata title="My Jeopardy Game" author="Author" language="en"/>
</gamePackage>
```

The loader expects `game.xml` to contain a title and description, followed by one or more rounds.
Each round declares its board dimensions. Questions use zero-based `row` and `category`
coordinates:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<game version="1.0">
  <meta>
    <title>My Jeopardy Game</title>
    <description>A custom Jeopardy board.</description>
  </meta>
  <resources>
    <!-- Optional media resources consumed by question media references. -->
  </resources>
  <rounds>
    <round name="Round 1" categoryCount="2" questionCount="2">
      <categories>
        <category row="0">Science</category>
        <category row="1">History</category>
      </categories>
      <questions>
        <question row="0" category="0" points="100" type="mcq">
          <prompt>What is H2O?</prompt>
          <options>
            <option correct="true">Water</option>
            <option>Oxygen</option>
            <option>Hydrogen</option>
          </options>
        </question>
      </questions>
    </round>
  </rounds>
</game>
```

Every board position must contain exactly one question. Multiple-choice questions (`type="mcq"`)
must provide options and exactly one option marked `correct="true"`. Non-multiple-choice questions
may omit the options element. Media paths are resolved relative to the game package and their type
is detected from the file content.

## Source layout

```text
src/main/java/quantumquest/triviathrone/jeopardy/
├── action/       Action definitions and dispatch
├── creator/      Game-package creation
├── definition/   XML loading and game definitions
├── dto/          Messages and game state
├── resource/     Package-resource and media handling
└── session/      Board, scoring, buzzer, and session logic
```

The browser interfaces are in `src/main/resources/ui/`. Package examples are in
`src/main/resources/game-templates/`.

## License

This project is distributed under the MIT License. See [LICENSE](LICENSE).

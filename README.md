![Screenshot](.github/images/header.png)

# BuildSystem ![GitHub Workflow Status (branch)](https://img.shields.io/github/workflow/status/einTosti/BuildSystem/Build%20main/master) ![version](https://img.shields.io/github/v/release/einTosti/BuildSystem) [![CodeFactor](https://www.codefactor.io/repository/github/eintosti/buildsystem/badge)](https://www.codefactor.io/repository/github/eintosti/buildsystem) [![Discord](https://img.shields.io/discord/419460301403193344.svg?label=&logo=discord&logoColor=ffffff&color=7389D8&labelColor=6A7EC2)](https://discord.com/invite/Nt467Rf)

## Table of contents

* [Introduction](#introduction)
* [Full guide](#full-guide)
* [Features](#features)
* [Screenshots](#screenshots)
* [Contributing](#contributing)
    * [Compilation](#compilation)
    * [PR Policy](#pr-policy)
* [License](#license)

## Introduction

**BuildSystem** is a simple but powerful - as the name already says - system for builders, with lots of great features
for everyday usage. Manage worlds in the worlds navigator, change their permission, projects and status with ease. And
not to forget: let each player decide which settings whey think are best for them and now the building can start!

## Full guide

You can find a full guide with tutorials, commands and permissions [here](https://eintosti.gitbook.io/buildsystem/).

## Features

- Fancy GUIs
- 100% customisable messages
- Customisable scoreboard
- Powerful navigator which allows for an overview of all worlds
    - Extra GUIs for:
        - Not started, In progress, almost finished and finished maps
        - Maps that have been put to the archive
        - Private player maps (each player can create their own map)
          But only if a map with their name doesn't exist
- Create worlds with ease: When creating a world, choose from:
    - Predefined worlds _or_
    - Custom generators provided by 3rd party plugins _or_
    - Custom templates which you can add yourself
- Easily manage your worlds
    - Choose from over 6 different statuses for each world
        - from _**not started**_, to _**in progress**_, over _**almost finished**_, continuing to _**finished**_, not to
          forget _**archive**_ and finally _**hidden**_
- When in worlds that are set as finished, the player is invisible and can fly in adventure mode, so they can only have
  a look without breaking anything [bypass: `/build`]
- Set the permission you need to join a worlds
- Change what project each world is for (e.g. `"A small BedWars map"`)
- Set the world item, so you can spot them faster among other worlds
    - You can choose from all items that are available in Minecraft
- Import, delete and create worlds with ease
- New and improved navigator!
    - Not a GUI anymore, but furthermore an interactive navigator
    - But if you rather have a "normal" GUI, the option is there for you to toggle between the
- Per player settings
- Building tools
    - Toggle block physics
    - Custom speed
    - Receive player skulls
    - Change the time in a world with only a single command
    - Toggle double slab breaking
    - No-Clip
    - Open iron doors and iron trapdoors
    - Night vision
    - Disable interactions with certain blocks

## Screenshots

### Navigator

![Screenshot](.github/images/navigator.png)

### World Navigator

![Screenshot](.github/images/worlds.png)

### Create predefined worlds

![Screenshot](.github/images/predefined_worlds.png)

### Create worlds with the help of custom templates

![Screenshot](.github/images/templates.png)

### Receive secret blocks

![Screenshot](.github/images/blocks.png)

### Change which status each world has

![Screenshot](.github/images/status.png)

### Per player settings

![Screenshot](.github/images/settings.png)

### Change the default items

![Screenshot](.github/images/setup.png)

### Custom scoreboard

![Screenshot](.github/images/scoreboard.png)

## Contributing

### Compilation

Build with `mvn clean install`.

### PR Policy

I'll accept changes that make sense. You should be able to justify their existence, along with any maintenance costs
that come with them.

## License

This project is licensed under the [BSD 4-Clause License](LICENSE).

InventoryRemover
================

Banned items are removed from player and chest inventories.

To add an item to the banned list, open the config.yml file in /plugins/InventoryRemover/config.yml and add the item to the list.

To ensure that the config file will be loaded into memory correctly, you may wish to check your YAML in an online parser, like the one here: http://yaml-online-parser.appspot.com/.

==============

Example config.yml:

bannedItems:
  inventory:
  - 1
  - 2
  - 4
  - 100
  world:
  - 30
  - 31
  - 50

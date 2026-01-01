# BlockyFile

[![CurseForge Downloads](https://img.shields.io/curseforge/dt/1408303?style=for-the-badge&logo=curseforge&label=CurseForge%20Downloads&color=F16436)](https://www.curseforge.com/minecraft/mc-mods/blocky-file)\
[![Modrinth Downloads](https://img.shields.io/modrinth/dt/U4NEshfC?style=for-the-badge&logo=modrinth&label=Modrinth%20Downloads&color=00AF5C)](https://modrinth.com/mod/blocky-file)\
[![MC百科](https://img.shields.io/badge/mcmod.cn-MC%E7%99%BE%E7%A7%91-58b6d8?style=for-the-badge)](https://www.mcmod.cn/class/23823.html)

A client-side mod that uses blocks to store files in the Minecraft world. In-game configuration GUI is supported when Cloth Config is installed.

**This mod only accesses files explicitly selected by the user in the command argument. Please note that you should not share files containing sensitive personal information!**

This mod allows you to store files from your computer into blocks in the Minecraft world and recover those files from those blocks. The mod uses a configurable block palette to map file bits to Minecraft blocks.

With this mod, you can treat Minecraft as a binary file viewer and editor, visualizing the bytes of a file or editing it by changing blocks. You can also save files to game saves, use them as Easter eggs, or as a fun way to transfer files, and even share files with friends in multiplayer mode!

In single-player mode, this mod directly manipulates the server world to place and load blocks. Otherwise, it uses `/setblock` to place blocks and loads them from the client world.

Supported Minecraft versions:

- Fabric 1.21.4~1.21.11
- NeoForge/Forge 1.21.6~1.21.11

## Commands

- Store a file as blocks / Read a file from blocks:
  ```mcfunction
  blockyfile file <store|load> <x1> <y1> <z1> <x2> <y2> <z2> <axisOrder> <fileName>
  ```

- Reload the configuration file:
  ```mcfunction
  blockyfile config reload
  ```

- Import block palette from the clipboard / Export block palette to the clipboard:
  ```mcfunction
  blockyfile palette <import|export> clipboard
  ```

- Import block palette from a file / Export block palette to a file:
  ```mcfunction
  blockyfile palette <import|export> file <fileName>
  ```

- Import block palette from blocks in the world / Export block palette to blocks in the world:
  ```mcfunction
  blockyfile palette <import|export> world <x1> <y1> <z1> <x2> <y2> <z2> <axisOrder>
  ```

## Thanks

Thanks to [Xiamo-vip/WoolFileStorage-1.21.10](https://github.com/Xiamo-vip/WoolFileStorage-1.21.10) for the inspiration.

Its video on Bilibili: [将文件转储在我的世界里_哔哩哔哩bilibili_Minecraft_游戏集锦](https://www.bilibili.com/video/BV1bKmnBdEBb)
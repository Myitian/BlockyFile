# BlockyFile

A client-side mod that uses blocks to store files in the Minecraft world. In-game configuration GUI is supported when Cloth Config is installed.

In single-player mode, this mod directly manipulates the server world to place and load blocks. Otherwise, it uses `/setblock` to place blocks and loads them from the client world.

Supported Minecraft versions:

- Fabric 1.21.4~1.21.11
- NeoForge/Forge 1.21.6~1.21.11

## Commands

- Store a file as blocks / Read a file from blocks:
  ```mcfunction
  blockyfile file <store|load> <x1> <y1> <z1> <x2> <y2> <z2> <axisOrder> <filename>
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
  blockyfile palette <import|export> file <filename>
  ```

- Import block palette from blocks in the world / Export block palette to blocks in the world:
  ```mcfunction
  blockyfile palette <import|export> world <x1> <y1> <z1> <x2> <y2> <z2> <axisOrder>
  ```

## Thanks

Thanks to [Xiamo-vip/WoolFileStorage-1.21.10](https://github.com/Xiamo-vip/WoolFileStorage-1.21.10) for the inspiration.

Its video on Bilibili: [将文件转储在我的世界里_哔哩哔哩bilibili_Minecraft_游戏集锦](https://www.bilibili.com/video/BV1bKmnBdEBb)
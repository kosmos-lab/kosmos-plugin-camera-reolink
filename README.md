# KosmoS Reolink camera Plugin 



# Installation

## compile yourself

If you want to compile it yourself you can do so with

```shell
mvn clean deploy
```

This should create a zip in the target folder.

Just drop this zip file to the "plugins" folder of the KosmoS platform.



## Get release from github

If you dont want to compile yourself you can always get the zip file from the latest [releases](https://github.com/kosmos-lab/kosmos-plugin-camera-reolink/releases/latest) page and drop it into the "plugins" folder of the KosmoS platform.

## Configure

Open your KosmoS config file (config/config.json).

Find or add the "camera" block and change it to the correct settings.

```json
{
    [...]
  "cameras":[
    {
      "password":"verysecure",
      "name":"reolink-camera-1",
      "base":"http://192.168.1.123/",
      "username":"admin",
      "clazz":"de.kosmos_lab.platform.plugins.camera.reolink.ReolinkCamera"
    }
  ],
    [...]
}
```








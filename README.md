MFileChooser
============

Cordova/Phonegap plugin that supplies a File Chooser for Android 4+. The File Chooser does not require installation external file manager

![alt text](http://i1204.photobucket.com/albums/bb408/krestor85/71df8523-0e50-48d0-a397-b00498910293_zpsd97df182.png "Screenshot 1") | ![alt text](http://i1204.photobucket.com/albums/bb408/krestor85/d11eda8a-8c37-4d9c-8890-eb96cfd07977_zpsdd9f0bf3.png "Screenshot 2")

## Limitation

Currently, this plugin only support an image format.

## Adding the Plugin to your project ##

Plugin has been successfully tested on Android 4+ device with Cordova 3.

To install the plugin

```
cordova plugin add --save cordova-plugin-dotneet-mfilechooser
```

## Using the plugin ##

The plugin creates the object `window.plugins.mfilechooser`. To use, call the `open()` method:

### Simple using ###

```javascript
   window.plugins.mfilechooser.open([], function (uri) {
      
      alert(uri);
      
    }, function (error) {
      
        alert(error);
    
    });
```

### Filtering by extension ###

```javascript
   window.plugins.mfilechooser.open(['.doc', '.xls', '.ppt'], function (uri) {
      
      alert(uri);
      
    }, function (error) {
      
        alert(error);
    
    });
```

### File Upload

Below is the file upload example by ES2016.
(It depend on cordova-plugin-file-transfer)

```javascript
window.plugins.mfilechooser.open(['.jpg', '.jpeg', '.png'], (url) => {
  window.resolveLocalFileSystemURL(url, (entry) => {
    const ft = new FileTransfer();
    const success = () => console.log('success')
    const fail = () => console.log('fail')
    ft.upload(entry.toURL(), encodeURI('http://example.com/upload.php'), success, fail, {})
  })
})
```

## Release notes ##

September 16, 2014 Initial release

## Contribution ##

This plugin based on follow repository:

 - https://github.com/ingyesid/simple-file-chooser (by ingyesid)
 - https://github.com/roberthovhannisyan/MFileChooser (by roberthovhannisyan)


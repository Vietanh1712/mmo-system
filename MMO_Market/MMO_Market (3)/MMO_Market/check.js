var fso = new ActiveXObject("Scripting.FileSystemObject");
var f = fso.OpenTextFile("d:/mmo-system/MMO_Market/MMO_Market (3)/MMO_Market/apps/frontend/static/js/seller/seller-console.js", 1);
var content = f.ReadAll();
f.Close();
try {
    eval(content);
    WScript.Echo("OK");
} catch(e) {
    WScript.Echo("Error: " + e.message);
}

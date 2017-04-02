
var filePath=""

module.exports = function(RED) {
	function translate(value) {
	    return ("0" + value).slice(-2)
	}

	function getFileName(id,extention) {
		var util = require("util");
		var date = new Date(Date.now());
		if(filePath=="") return filePath;
		if(filePath.slice(-1)=="/") {
			filePath = filePath.slice(0,filePath.length-1);
		}
		if(id) id=String(id);
		else id=""
		var fileName = util.format("%s/%d%s%s%s%s.%s",
			filePath,
			date.getFullYear(),
			translate(date.getMonth() + 1),
			translate(date.getDate()),
			translate(date.getHours()),
			id,
			extention);
		return fileName;
	}

	function patlamp(config) {
		RED.nodes.createNode(this,config);
		var node = this;
		filePath=config.filePath;

		this.on('input', function(msg) {
			var newMsg = {};
			var fileName = getFileName(msg.id,"csv");
			if(fileName != "") {
				newMsg.filename = fileName;
				newMsg.payload = msg.payload;
			}
			node.send(newMsg);
		});
	}
	RED.nodes.registerType("patlamp",patlamp);
    RED.httpAdmin.post("/patlamp/:id/:state", RED.auth.needsPermission("patlamp.write"), function(req,res) {
        var node = RED.nodes.getNode(req.params.id);
        var state = req.params.state;
        if (node !== null && typeof node !== "undefined" ) {
            if (state === "enable") {
                node.active = true;
                res.sendStatus(200);
            } else if (state === "disable") {
                node.active = false;
                res.sendStatus(201);
            } else {
                res.sendStatus(404);
            }
        } else {
            res.sendStatus(404);
        }
    });

    // the auth header attached. So do not use RED.auth.needsPermission here.
}



module.exports = function(RED) {
	var lib;
	var stream = require('stream');
	var util = require('util');

	function Warn(message){
		RED.log.warn("patlamp: " + message);
	}

	function connect(node) {
		if(!node.opened) {
			console.log("node.open");
			if(!lib.dlopen(node.libFile)) { Warn("dlopen fail"); return false; }
			console.log("success lib.dlopen");
			if(!lib.patlamp_init()) { Warn("patlamp_init fail"); return false; }
			console.log("success lib.patlamp_init");
			node.status({fill:"green",shape:"dot",text:"connected"},true);
		}
		return true;
	}

	function disconnect(node) {
		if(node.opened) {
			if(!lib.patlamp_remove()) { Warn("lazurite_rxDisable fail."); }
			if(!lib.dlclose()) { Warn("dlclose fail"); }
		}
		node.status({fill:"red",shape:"ring",text:"disconnected"});
		return false;
	}

	function translate(value) {
	    return ("0" + value).slice(-2)
	}
	function patlamp_init(node) {
		console.log(node);
		console.log("setMapfile:: "+node.mapFile);
		lib.patlamp_setMapfile(node.mapFile);
		console.log("setReportInterval:: "+ node.reportInterval);
		lib.patlamp_setReportInterval(node.reportInterval);
		console.log("setDetectInterval:: "+ node.detectInterval);
		lib.patlamp_setDetectInterval(node.detectInterval);
		node.disp = lib.patlamp_getDisplay();
		return;
	}

	function patlamp_cam(config) {
		RED.nodes.createNode(this,config);
		var node = this;
		lib = require('./build/Release/patlamp_wrap');
		this.libFile =config.libFile;
		this.mapFile =config.mapFile;
		this.reportInterval =config.reportInterval;
		this.detectInterval =config.detectInterval;
		this.opened = false;
		this.opened = connect(node);
		if(!this.opened) { Warn("[patlamp-cam] open error"); }
		else {console.log("[patlamp-cam]success!");}
		this.timer = null;
		this.interval=1000;
		if(this.disp == undefined) this.disp = false;
		console.log(lib);
		patlamp_init(node);
		this.timer = setInterval(function() {
			if(this.disp) {
				if(!this.active){
					console.log("disp off")
					lib.patlamp_setDisplay(false);
					this.disp = false;
				}
			} else {
				if(this.active) {
					console.log("disp on")
					lib.patlamp_setDisplay(true);
					this.disp = true;
				}
			}
			var msg ={};
			msg.payload = lib.patlamp_readData();
			if(msg.payload != "") node.send(msg);
		}.bind(this),this.interval);
		this.opened=true;

		node.on('close', function(done) {
//			if(this.opened) {
				if(this.timer) clearInterval(this.timer);
//				disconnect(node);
				done();
//			}
//			this.opened = false;
			console.log("node.close");
		});
	}

	function patlamp_photo(config) {
		RED.nodes.createNode(this,config);
		var node = this;
		this.path=config.path;

		node.on('input', function(msg) {
			console.log(msg);
		});
		node.on('close', function(done) {
			disconnect(node);
			done();
			delete require.cache[require.resolve('./build/Release/patlamp_wrap')];
		});
	}

	RED.nodes.registerType("patlamp-cam",patlamp_cam);
	RED.nodes.registerType("patlamp-photo",patlamp_photo);
    RED.httpAdmin.post("/patlamp_cam/:id/:state", RED.auth.needsPermission("patlamp-cam.write"), function(req,res) {
        var node = RED.nodes.getNode(req.params.id);
        var state = req.params.state;
        if (node !== null && typeof node !== "undefined" ) {
            if (state === "enable") {
                node.active = true;
				this.active = true;
                res.sendStatus(200);
            } else if (state === "disable") {
                node.active = false;
				this.active = false;
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

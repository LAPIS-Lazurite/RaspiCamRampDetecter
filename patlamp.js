

module.exports = function(RED) {
	var lib;
	var opened = false;
	var stream = require('stream');
	var util = require('util');

	function Warn(message){
		RED.log.warn("patlamp: " + message);
	}

	function connect(node) {
		node.status({fill:"red",shape:"ring",text:"disconnected"});
		if(!opened) {
			lib = require('./build/Release/patlamp_wrap');
			if(!lib.dlopen(node.libFile)) { Warn("dlopen fail"); return false; }
			if(!lib.patlamp_init()) { Warn("patlamp_init fail"); return false; }
			node.status({fill:"green",shape:"dot",text:"connected"},true);
			opened = true;
		} else {
			node.status({fill:"green",shape:"dot",text:"connected"},true);
		}
		return true;
	}

	function disconnect(node) {
		if(opened) {
			if(!lib.patlamp_remove()) { Warn("lazurite_rxDisable fail."); }
			if(!lib.dlclose()) { Warn("dlclose fail"); }
			opened = false;
		}
		node.status({fill:"red",shape:"ring",text:"disconnected"});
		return false;
	}

	function translate(value) {
	    return ("0" + value).slice(-2)
	}
	function patlamp_open() {
		if(!opened) lib = require('./build/Release/patlamp_wrap');
	}
	function patlamp_init(node) {
		lib.patlamp_setMapfile(node.mapFile);
		lib.patlamp_setReportInterval(node.reportInterval);
		lib.patlamp_setDetectInterval(node.detectInterval);
		lib.patlamp_setExpandMag(node.expandMag);
		node.disp = lib.patlamp_getDisplay();
		return;
	}

	function patlamp_cam(config) {
		RED.nodes.createNode(this,config);
		var node = this;
		this.libFile =config.libFile;
		this.mapFile =config.mapFile;

		if (typeof config.reportInterval === 'string' || config.reportInterval instanceof String) {
			this.reportInterval = parseInt(config.reportInterval);
		} else {
			this.reportInterval = config.reportInterval;
		}
		if (typeof config.detectInterval === 'string' || config.detectInterval instanceof String) {
			this.detectInterval = parseInt(config.detectInterval);
		} else {
			this.detectInterval = config.detectInterval;
		}
		if (typeof config.expandMag === 'string' || config.expandMag instanceof String) {
			this.expandMag = parseInt(config.expandMag);
		} else {
			this.expandMag = config.expandMag;
		}
		opened = connect(node);
		if(!opened) { Warn("[patlamp-cam] open error"); }
		else {console.log("[patlamp-cam]success!");}
		this.timer = null;
		this.interval=1000;
		if(this.disp == undefined) this.disp = false;
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
		opened=true;

		node.on('close', function(done) {
//			if(this.opened) {
				if(this.timer) clearInterval(this.timer);
//				disconnect(node);
				done();
//			}
//			this.opened = false;
		});
	}

	function patlamp_photo(config) {
		RED.nodes.createNode(this,config);
		var node = this;
		connect(node);
		this.path=config.path;
		this.extention="jpg";

		node.on('input', function(msg) {
			var util = require("util");
			var date = new Date(Date.now());
			if(this.path=="") return false;
			if(this.path.slice(-1)=="/") {
				this.path = this.path.slice(0,this.path.length-1);
			}
			var fileName = util.format("%s/%d%s%s%s%s%s%s.%s",
				this.path,
				date.getFullYear(),
				translate(date.getMonth() + 1),
				translate(date.getDate()),
				translate(date.getHours()),
				translate(date.getMinutes()),
				translate(date.getSeconds()),
				translate(date.getMilliseconds()),
				this.extention);
			lib.patlamp_snapShot(fileName);
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

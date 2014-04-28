(function(){
	if (window.mapleque===undefined)
		mapleque={};
	if (mapleque.evilbomb!==undefined)
		return;
	
	var _randV=function(num){
		return Math.floor(Math.random()*num);
	};
	var init=function(conf){
		console.log("游戏初始化");
		var _self=this;
		conf=conf||{};
		_self.w=conf.x||10;
		_self.h=conf.y||10;
		_self.n=conf.colorNum||6;
		_self.flush=conf.flush||0;
		
		for (var x=0;x<_self.w;x++){
			_self.desk[x]=[];
			for (var y=0;y<_self.h;y++){
				_self.desk[x][y]=_randV(_self.n);
			}
		}
		if (typeof(_self.flush)=='function')_self.flush();
		while (!check(_self));
		//check(_self);
	};
	var change=function(x1,y1,x2,y2){
		console.log("交换两个元素的位置：",x1,y1,"<=>",x2,y2);
		if (Math.abs(x1-x2)>1||Math.abs(y1-y2)>1){
			console.log("不相邻的两个元素不能交换位置");
			return;
		}
		var _self=this;
		var tmp=_self.desk[x1][y1];
		_self.des[x1][y1]=_self.desk[x2][y2];
		_self.desk[x2][y2]=tmp;
	};
	var check=function(_self){
		console.log("检查是否存在可消除元素");
		if (_self===undefined)
			_self=this;
		var desk=_self.desk;
		for (var x in desk){
			for (var y in desk[x]){
				var samex=[[x,y]],samey=[[x,y]];
				(function(x,y,w,h){
					x=parseInt(x);
					y=parseInt(y);
					if (w&&desk[x+1]&&desk[x+1][y]&&desk[x+1][y]==desk[x][y]){
						samex.push([x+1,y]);
						arguments.callee(x+1,y,1,0);
					}
					if (h&&desk[x][y+1]&&desk[x][y+1]==desk[x][y]){
						samey.push([x,y+1]);
						arguments.callee(x,y+1,0,1);
					}
				})(x,y,1,1);
				if (samex.length>2&&samey.length>2){
					//special bomb
					bomb(samex.concat(samey),_self);
					return false;
				}else if (samex.length>2){
					bomb(samex,_self);
					return false;
				}else if (samey.length>2){
					bomb(samey,_self);
					return false;
				}else{
					//do nothing;
				}
			}
		}
		return true;
	};
	
	var bomb=function(arr,_self){
		console.log("_self",_self,this);
		if (_self===undefined)
			_self=this;
		console.log("消除指定元素",arr);
		for (var i in arr){
			console.log("当前元素下移",arr[i]);
			var mx=arr[i][0];
			var my=arr[i][1];
			for (var y=my;y>0;y--){
				console.log(_self.desk,mx,y);
				_self.desk[mx][y]=_self.desk[mx][y-1];
			}
			console.log("顶部添加新的元素");
			_self.desk[mx][0]=_randV(_self.n);
		}
		if (typeof(_self.flush)=='function')_self.flush();
	};
	var evilbomb=function(){
		this.desk=[];
		this.conf={};
	};
	evilbomb.prototype={
			constructor:evilbomb,
			init:init,
			change:change,
			check:check
	};
	mapleque.evilbomb=evilbomb;
})();
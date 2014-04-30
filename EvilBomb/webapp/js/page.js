(function() {
	var eb = new mapleque.evilbomb();
	eb.init({
		flush : function() {
			console.log("flush");
			var html = [];
			html.push('<div>');
			for ( var x in eb.desk) {

				for ( var y in eb.desk[x]) {
					html.push('<div id="cup-' + x +'%'+ y
							+ '" class="cup" dropallow="">');
					html.push('<div draggable="true" id="' + x +'%'+ y
							+ '" class="ele color-' + eb.desk[x][y] + '">'
							+ '&nbsp;' + '</div>');
					html.push('</div>');
				}

			}
			html.push('</div>');
			var desk = document.getElementById("desk");
			desk.innerHTML = html.join("");

			// TODO:添加操作方法，调用eb.change(x1,y1,x2,y2)
			var canChange=function(x1,y1,x2,y2){
				var dx=Math.abs(x1-x2);
				var dy=Math.abs(y1-y2);
				return dx+dy<2&&dx+dy>0;
			};
			
			var allowDrop=function(ev) {
				ev.preventDefault();
			};

			var drag=function (ev) {
				console.log("drag tar",ev.target);
				ev.dataTransfer.setData("from-id", ev.target.id);
			};

			var drop=function (ev) {
				console.log("drop tar",ev.target);
				ev.preventDefault();
				var fromId = ev.dataTransfer.getData("from-id");
				var toId=ev.target.id;
				var x1y1=fromId.split("%");
				var x2y2=toId.split("%");
				var x1=parseInt(x1y1[0]),y1=parseInt(x1y1[1]);
				var x2=parseInt(x2y2[0]),y2=parseInt(x2y2[1]);
				if (!canChange(x1,y1,x2,y2))return;
				eb.change(x1,y1,x2,y2);
			};
			
			var eles=document.querySelectorAll('.ele');
			[].forEach.call(eles,function(ele){
				ele.addEventListener('drop',drop);
				ele.addEventListener('dragstart',drag);
				ele.addEventListener('dragover',allowDrop);
			});
		}
	});
})();

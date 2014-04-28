(function() {
	var eb = new mapleque.evilbomb();
	eb.init({
		flush:function() {
			console.log("flush");
			var html = [];
			html.push('<table>');
			for ( var x in eb.desk) {
				html.push('<tr>');
				for ( var y in eb.desk[x]) {
					html.push('<td class="color-' + eb.desk[x][y] + '">'
							+ eb.desk[x][y] + '</td>');
				}
				html.push('</tr>');
			}
			html.push('</table>');
			var desk = document.getElementById("desk");
			desk.innerHTML = html.join("");
			
			//TODO:添加操作方法，调用eb.change(x1,y1,x2,y2)
		}
	});
})();
<script type="text/javascript" >
function bc_wrapline_textarea(targetElemId, sourceElemId) {
var targetElem = document.getElementById(targetElemId);
var sourceElem = document.getElementById(sourceElemId);
if (targetElem.wrap == "off") {
sourceElem.innerHTML = "[-]";
targetElem.wrap = "on";
}
else {
sourceElem.innerHTML = "[+]";
targetElem.wrap = "off";
}
}
</script>

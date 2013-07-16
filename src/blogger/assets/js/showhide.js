<script type="text/javascript" >
function showhide(targetElemId, sourceElemId) {
var targetElem = document.getElementById(targetElemId);
var sourceElem = document.getElementById(sourceElemId);
if (targetElem.style.display == "none") {
sourceElem.innerHTML = "[-]";
targetElem.style.display = "block";
}
else {
sourceElem.innerHTML = "[+]";
targetElem.style.display = "none";
}
}
</script>

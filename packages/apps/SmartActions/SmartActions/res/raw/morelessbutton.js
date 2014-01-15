// JavaScript Document

function toggle(id, link) {
    var e = document.getElementById(id);

    if (e.style.display == 'inline') {
        e.style.display = 'none';
    } else {
        e.style.display = 'inline';
        fade(id);
    }
}

function toggleFade(id, link) {
    var element = document.getElementById(id);
    if (element == null) return;

    if (element.FadeState == null) element.FadeState = -2;

    if (element.FadeState == 1 || element.FadeState == -1) {
        element.FadeState = element.FadeState == 1 ? -1 : 1;
        element.FadeTimeLeft = TimeToFade - element.FadeTimeLeft;
    } else {
        element.FadeState = element.FadeState == 2 ? -1 : 1;
        element.FadeTimeLeft = TimeToFade;
        setTimeout("animateFade(" + new Date().getTime() + ",'" + id + "')", 33);
    }
}
var TimeToFade = 300.0;

function animateFade(lastTick, id) {
    var curTick = new Date().getTime();
    var elapsedTicks = curTick - lastTick;

    var element = document.getElementById(id);
    var arrowElement = document.getElementById("arrow");

    if (element.FadeTimeLeft <= elapsedTicks) {
        if (element.FadeState == 1) {
            element.style.opacity = 1;
            element.FadeState = 2;
            arrowElement.style.webkitTransform = "rotate(-180deg)";
        } else {
            element.style.opacity = 0;
            element.FadeState = -2;
            element.style.display = 'none';
            arrowElement.style.webkitTransform = "rotate(0deg)";
        }
        return;
    }
    element.style.display = 'block';

    element.FadeTimeLeft -= elapsedTicks;
    var newOpVal = element.FadeTimeLeft/TimeToFade;
    if (element.FadeState == 1) newOpVal = 1 - newOpVal;

    element.style.opacity = newOpVal;
    var angle = newOpVal * -180;
    arrowElement.style.webkitTransform = "rotate(" + angle + "deg)";

    setTimeout("animateFade(" + curTick + ",'" + id + "')", 33);
}


function reload() {
    history:go(0);
}

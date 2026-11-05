// 게임보다 먼저 실행되는 휴대폰 지원 코드입니다.
(function () {
  function lockLandscape() {
    if (screen.orientation && screen.orientation.lock) {
      screen.orientation.lock("landscape").catch(function () {});
    }
  }

  // 안드로이드처럼 전체화면을 지원하는 기기는 터치할 때 전체화면으로 바꿉니다. 아이폰은 지원하지 않습니다.
  document.addEventListener("touchend", function () {
    var page = document.documentElement;
    var request = page.requestFullscreen || page.webkitRequestFullscreen;
    if (!request || document.fullscreenElement || document.webkitFullscreenElement) return;
    try {
      var result = request.call(page);
      if (result && result.then) result.then(lockLandscape, function () {});
    } catch (e) { /* 브라우저가 거절하면 그대로 진행합니다. */ }
  }, { passive: true });

  // iOS는 화면을 돌리거나 주소창이 줄어든 직후의 창 크기가 늦게 갱신됩니다.
  // 잠시 뒤 게임에 크기를 다시 알려 화면 위쪽이 잘리지 않게 합니다.
  var resending = false;
  function resendSize() {
    window.scrollTo(0, 0);
    resending = true;
    window.dispatchEvent(new Event("resize"));
    resending = false;
  }
  function settle() {
    if (resending) return;
    [80, 300, 800].forEach(function (delay) {
      setTimeout(resendSize, delay);
    });
  }
  window.addEventListener("resize", settle);
  window.addEventListener("orientationchange", settle);
  window.addEventListener("pageshow", settle);
  if (window.visualViewport) window.visualViewport.addEventListener("resize", settle);
})();

(function () {
  const path = (window.location.pathname || "").replace(/\\/g, "/");
  const file = path.split("/").pop() || "index.html";
  const isHome = file === "" || file === "index.html" || file === "/";

  function active(href) {
    const name = href.replace("./", "");
    if (isHome && (name === "index.html" || name === "")) return " is-active";
    return file === name ? " is-active" : "";
  }

  const header = `
  <header class="site-header">
    <div class="wrap nav">
      <a class="brand" href="./index.html" aria-label="쉬운가계부 홈">
        <img src="./assets/icon.png" alt="" width="36" height="36" />
        쉬운가계부
      </a>
      <button class="nav-toggle" type="button" aria-label="메뉴 열기" id="navToggle">☰</button>
      <nav class="nav-links" id="navLinks">
        <a class="${active("news.html").trim()}" href="./news.html">새 소식</a>
        <a class="${active("about.html").trim()}" href="./about.html">앱 소개</a>
        <a class="${active("features.html").trim()}" href="./features.html">기능</a>
        <a class="${active("guide.html").trim()}" href="./guide.html">사용 방법</a>
        <a class="${active("faq.html").trim()}" href="./faq.html">FAQ</a>
        <a class="nav-cta" href="./index.html#download">다운로드</a>
      </nav>
    </div>
  </header>`;

  const footer = `
  <footer class="site-footer">
    <div class="wrap footer-columns">
      <div class="footer-col">
        <h3>쉬운가계부</h3>
        <p>로그인 없이 이 폰 안에서만 쓰는 개인 가계부예요. 빠른 입력, 달력·내역·통계, 고정비와 홈 위젯으로 오늘 쓴 돈을 가볍게 남기고 한 달을 단정하게 돌아보세요. 완전 무료.</p>
      </div>
      <div class="footer-col">
        <h3>바로가기</h3>
        <ul>
          <li><a href="./news.html">새 소식</a></li>
          <li><a href="./about.html">앱 소개</a></li>
          <li><a href="./features.html">기능</a></li>
          <li><a href="./guide.html">사용 방법</a></li>
          <li><a href="./data.html">데이터 안내</a></li>
          <li><a href="./faq.html">FAQ</a></li>
        </ul>
      </div>
      <div class="footer-col">
        <h3>약관 및 정책</h3>
        <ul>
          <li><a href="./privacy.html">개인정보 처리방침</a></li>
          <li><a href="./terms.html">이용약관</a></li>
          <li><a href="./permissions.html">앱 권한 안내</a></li>
          <li><a href="./contact.html">문의하기</a></li>
        </ul>
      </div>
      <div class="footer-col">
        <h3>앱 정보</h3>
        <ul class="footer-meta">
          <li><span>플랫폼</span> Android (Google Play)</li>
          <li><span>이용 요금</span> 무료</li>
          <li><span>언어</span> 한국어</li>
          <li><span>패키지</span> com.sunwoo.easyledger</li>
          <li><span>문의</span> <a href="mailto:onamt@naver.com">onamt@naver.com</a></li>
        </ul>
      </div>
    </div>
    <div class="wrap footer-bottom">
      <span>© SUNWOO · 쉬운가계부</span>
      <a href="./index.html">홈으로</a>
    </div>
  </footer>`;

  const headerMount = document.getElementById("site-header");
  const footerMount = document.getElementById("site-footer");
  if (headerMount) headerMount.outerHTML = header;
  if (footerMount) footerMount.outerHTML = footer;

  const toggle = document.getElementById("navToggle");
  const links = document.getElementById("navLinks");
  toggle?.addEventListener("click", () => links?.classList.toggle("open"));
  links?.querySelectorAll("a").forEach((a) => {
    a.addEventListener("click", () => links.classList.remove("open"));
  });
})();

# 쉬운가계부 소개 페이지

Vercel에 올리는 정적 랜딩 페이지입니다.

**사이트 URL (canonical):** `https://easyledger.sunoops.com/`  
도메인이 바뀌면 `index.html`·각 페이지 canonical/OG·`robots.txt`·`sitemap.xml`의 호스트를 함께 수정하세요.

## 로컬 확인

`website` 폴더의 `index.html`을 브라우저로 열거나:

```bash
npx serve website
```

## Vercel 배포

1. 이 저장소를 GitHub/GitLab 등에 push
2. [Vercel](https://vercel.com)에서 Import Project
3. **Root Directory**를 `website`로 지정
4. Framework Preset: Other (정적 파일)
5. Deploy

## 페이지 구성

| 경로 | 설명 |
| --- | --- |
| `/` | 홈(랜딩) |
| `/news.html` | 새 소식 |
| `/about.html` | 앱 소개 |
| `/features.html` | 기능 |
| `/guide.html` | 사용 방법 |
| `/data.html` | 데이터 안내 |
| `/faq.html` | FAQ |
| `/privacy.html` | 개인정보 처리방침 |
| `/terms.html` | 이용약관 |
| `/permissions.html` | 앱 권한 안내 |
| `/contact.html` | 문의하기 |

Play Store 링크는 `index.html`의 다운로드 버튼 URL을 스토어 등록 후 확인하세요.

현재 패키지: `com.sunwoo.easyledger`

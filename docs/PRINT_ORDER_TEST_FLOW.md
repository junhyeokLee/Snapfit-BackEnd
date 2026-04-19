# SnapFit Print Order Test Flow

## Goal

앨범 생성 후 실제 주문이 들어왔을 때 운영자가 인쇄소에 넘길 ZIP/PDF를 받고, 배송 상태를 `배송중` -> `배송완료`로 전환하는 테스트 흐름을 검증한다.

## Preconditions

- Backend `SNAPFIT_ORDER_ADMIN_KEY` 또는 관리자 콘솔 기본 키가 준비되어 있어야 한다.
- Flutter 앱에서 QA 버튼을 보려면 `--dart-define=ORDER_ADMIN_KEY=...`를 함께 넣어 실행한다.
- 관리자 콘솔은 `/admin/index.html` 또는 `/admin/v2.html`에서 주문 탭을 사용한다.

## User Flow

1. 앱에서 템플릿을 선택한다.
2. 앨범을 생성하고 사진/텍스트를 확인한다.
3. 주문 화면에서 수령인, 연락처, 주소, 결제수단을 입력한다.
4. 결제를 완료한다.
5. 서버는 주문을 `제작중(IN_PRODUCTION)`으로 전환하고 인쇄 패키지 링크를 준비한다.

## Operator Flow

1. 관리자 콘솔의 `주문 관리` 탭을 연다.
2. 주문을 검색하거나 `결제완료/제작중` 상태 주문을 확인한다.
3. `인쇄 준비`를 누른다.
4. `ZIP`을 다운로드한다.
5. ZIP 안의 `images/` 폴더와 `print-package.json`을 인쇄소 주문 등록에 사용한다.
6. 필요하면 `PDF`를 열어 페이지 순서와 이미지 누락 여부를 육안 확인한다.
7. 인쇄소 접수 후 송장번호가 나오면 택배사/운송장 번호를 입력하고 `배송중`을 누른다.
8. 실제 배송 완료 후 `배송완료`를 누른다.

## Print Package Contents

- `print-package.json`: 주문 정보, 수령인 정보, 앨범 비율, 페이지 순서, 원본/미리보기 이미지 URL, 레이어 JSON.
- `images/cover.*`: 커버 이미지.
- `images/page_001.*` 등: 페이지 순서대로 내려받은 인쇄용 이미지.
- `missing-assets.txt`: 다운로드하지 못한 이미지가 있을 때만 생성된다.

## Expected Status Sequence

1. `PAYMENT_PENDING`: 주문 생성 직후.
2. `IN_PRODUCTION`: 결제 완료 및 인쇄 접수 후.
3. `SHIPPING`: 송장 입력 후.
4. `DELIVERED`: 배송 완료 처리 후.

## Notes

- 현재 인쇄소 연동은 `MANUAL` 모드다. ZIP/PDF를 사람이 다운로드해서 인쇄소에 등록하는 방식이다.
- 다음 확장 단계는 ZIP/PDF를 Firebase Storage 또는 S3에 고정 업로드하고, 관리자 콘솔에서 항상 같은 다운로드 URL을 쓰도록 만드는 것이다.

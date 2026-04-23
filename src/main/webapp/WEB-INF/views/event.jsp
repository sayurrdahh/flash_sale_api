<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <title>선착순 쿠폰 이벤트</title>

    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>

    <style>
        body { font-family: 'Malgun Gothic', sans-serif; text-align: center; padding-top: 100px; }
        .event-box { border: 2px solid #ff5a5f; padding: 50px; display: inline-block; border-radius: 10px; }
        .issue-btn {
            background-color: #ff5a5f; color: white; border: none;
            padding: 15px 30px; font-size: 20px; font-weight: bold;
            cursor: pointer; border-radius: 5px;
        }
        .issue-btn:hover { background-color: #e0484d; }
    </style>
</head>
<body>

    <div class="event-box">
        <h2>🔥 선착순 100명 특별 쿠폰 🔥</h2>
        <p>지금 바로 클릭해서 쿠폰을 받아가세요!</p>
        <button id="issueBtn" class="issue-btn">쿠폰 응모하기</button>
    </div>

    <script>
        $(document).ready(function() {
            // 버튼 클릭 이벤트
            $('#issueBtn').on('click', function() {

                // 버튼 연타 방지 (클릭 시 즉시 비활성화)
                var $btn = $(this);
                $btn.prop('disabled', true).text('처리중...');

                // 테스트용 데이터 세팅 (실무에서는 세션이나 모델에서 로그인한 유저 ID를 가져옵니다)
                var couponId = 1; // 우리가 테스트 코드에서 세팅한 1번 쿠폰
                var userId = Math.floor(Math.random() * 10000); // 0~9999 사이의 랜덤 유저 ID 생성

                // 백엔드 API로 비동기(AJAX) 요청 전송
                $.ajax({
                    type: 'POST',
                    // 우리가 Controller에 만들어둔 V2(Redis) API 경로로 호출
                    url: '/api/v2/coupons/' + couponId + '/issue?userId=' + userId,

                    success: function(response) {
                        // HTTP 상태 코드 200 OK가 떨어지면 실행됨
                        alert("🎉 쿠폰 응모에 성공하셨습니다!");
                    },
                    error: function(xhr) {
                        // GlobalExceptionHandler에서 내려준 JSON 에러 응답 처리
                        if (xhr.responseJSON && xhr.responseJSON.message) {
                            alert("안내: " + xhr.responseJSON.message);
                        } else {
                            alert("일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
                        }
                    },
                    complete: function() {
                        // 성공하든 실패하든 처리가 끝나면 버튼을 다시 활성화
                        $btn.prop('disabled', false).text('쿠폰 응모하기');
                    }
                });
            });
        });
    </script>
</body>
</html>
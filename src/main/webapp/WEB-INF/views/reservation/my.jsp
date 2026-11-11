<%-- 내 예약 목록. 로그인한 회원의 예약만 보인다 (컨트롤러가 로그인 회원 id 로 조회). --%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>내 예약 - 회의실 예약</title>
    <style>
        body { font-family: sans-serif; max-width: 800px; margin: 40px auto; }
        table { width: 100%; border-collapse: collapse; }
        th, td { border-bottom: 1px solid #ddd; padding: 8px; text-align: left; }
        .info { color: #060; }
        .error { color: #c00; }
        form { display: inline; }
    </style>
</head>
<body>
<h1>내 예약</h1>

<%-- 신청 직후 한 번만 보이는 flash 메시지 --%>
<c:if test="${not empty successMessage}">
    <p class="info"><c:out value="${successMessage}"/></p>
</c:if>
<c:if test="${not empty errorMessage}">
    <p class="error"><c:out value="${errorMessage}"/></p>
</c:if>

<table>
    <thead>
    <tr><th>회의실</th><th>시간</th><th>사용 목적</th><th>상태</th><th></th></tr>
    </thead>
    <tbody>
    <c:forEach var="r" items="${reservations}">
        <tr>
            <td><c:out value="${r.roomName}"/></td>
            <td><c:out value="${r.timeRange}"/></td>
            <%-- 사용 목적은 신청자가 직접 입력한 값 → 반드시 이스케이프 --%>
            <td><c:out value="${r.purpose}"/></td>
            <td><c:out value="${r.status.label}"/></td>
            <td>
                <%-- r.cancellable → ReservationView.isCancellable() --%>
                <c:if test="${r.cancellable}">
                    <c:url var="cancelUrl" value="/reservations/${r.id}/cancel"/>
                    <form action="${cancelUrl}" method="post">
                        <%-- 일반 HTML form 은 CSRF 토큰을 직접 넣어야 한다.
                             _csrf 는 Spring Security 가 request 에 넣어둔 토큰 객체 --%>
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                        <button type="submit">취소</button>
                    </form>
                </c:if>
            </td>
        </tr>
    </c:forEach>
    <c:if test="${empty reservations}">
        <tr><td colspan="5">예약 내역이 없습니다.</td></tr>
    </c:if>
    </tbody>
</table>

<p><a href="<c:url value='/reservations/new'/>">예약 신청</a> | <a href="<c:url value='/'/>">처음으로</a></p>
</body>
</html>

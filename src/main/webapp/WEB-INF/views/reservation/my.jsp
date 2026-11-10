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
    </style>
</head>
<body>
<h1>내 예약</h1>

<%-- 신청 직후 한 번만 보이는 flash 메시지 --%>
<c:if test="${not empty successMessage}">
    <p class="info"><c:out value="${successMessage}"/></p>
</c:if>

<table>
    <thead>
    <tr><th>회의실</th><th>시간</th><th>사용 목적</th><th>상태</th></tr>
    </thead>
    <tbody>
    <c:forEach var="r" items="${reservations}">
        <tr>
            <td><c:out value="${r.roomName}"/></td>
            <td><c:out value="${r.timeRange}"/></td>
            <%-- 사용 목적은 신청자가 직접 입력한 값 → 반드시 이스케이프 --%>
            <td><c:out value="${r.purpose}"/></td>
            <td><c:out value="${r.status.label}"/></td>
        </tr>
    </c:forEach>
    <c:if test="${empty reservations}">
        <tr><td colspan="4">예약 내역이 없습니다.</td></tr>
    </c:if>
    </tbody>
</table>

<p><a href="<c:url value='/reservations/new'/>">예약 신청</a> | <a href="<c:url value='/'/>">처음으로</a></p>
</body>
</html>

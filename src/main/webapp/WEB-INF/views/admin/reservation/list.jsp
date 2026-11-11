<%-- 관리자 예약 관리. AdminReservationController 가 "admin/reservation/list" 를 반환하면 이 파일로 forward. --%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>예약 관리 - 회의실 예약</title>
    <style>
        body { font-family: sans-serif; max-width: 960px; margin: 40px auto; }
        table { width: 100%; border-collapse: collapse; }
        th, td { border-bottom: 1px solid #ddd; padding: 8px; text-align: left; }
        form { display: inline; }
        .info { color: #060; }
        .error { color: #c00; }
        .filter a { margin-right: 8px; }
        .filter .current { font-weight: bold; }
    </style>
</head>
<body>
<h1>예약 관리</h1>

<c:if test="${not empty successMessage}"><p class="info"><c:out value="${successMessage}"/></p></c:if>
<c:if test="${not empty errorMessage}"><p class="error"><c:out value="${errorMessage}"/></p></c:if>

<%-- 상태별 보기. statuses = ReservationStatus.values(), currentStatus 가 null 이면 전체 --%>
<p class="filter">
    <c:forEach var="s" items="${statuses}">
        <c:url var="filterUrl" value="/admin/reservations"><c:param name="status" value="${s}"/></c:url>
        <a href="${filterUrl}" class="${s == currentStatus ? 'current' : ''}"><c:out value="${s.label}"/></a>
    </c:forEach>
    <a href="<c:url value='/admin/reservations'/>" class="${empty currentStatus ? 'current' : ''}">전체</a>
</p>

<table>
    <thead>
    <tr><th>회의실</th><th>신청자</th><th>시간</th><th>사용 목적</th><th>상태</th><th>처리</th></tr>
    </thead>
    <tbody>
    <c:forEach var="r" items="${reservations}">
        <tr>
            <td><c:out value="${r.roomName}"/></td>
            <td><c:out value="${r.memberName}"/> (<c:out value="${r.loginId}"/>)</td>
            <td><c:out value="${r.timeRange}"/></td>
            <td><c:out value="${r.purpose}"/></td>
            <td><c:out value="${r.status.label}"/></td>
            <td>
                <%-- 일반 HTML form 이라 CSRF 토큰을 직접 넣는다 --%>
                <c:if test="${r.approvable}">
                    <c:url var="approveUrl" value="/admin/reservations/${r.id}/approve"/>
                    <form action="${approveUrl}" method="post">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                        <button type="submit">승인</button>
                    </form>
                </c:if>
                <c:if test="${r.status == 'PENDING'}">
                    <c:url var="rejectUrl" value="/admin/reservations/${r.id}/reject"/>
                    <form action="${rejectUrl}" method="post">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                        <button type="submit">거절</button>
                    </form>
                </c:if>
            </td>
        </tr>
    </c:forEach>
    <c:if test="${empty reservations}">
        <tr><td colspan="6">해당하는 예약이 없습니다.</td></tr>
    </c:if>
    </tbody>
</table>

<p><a href="<c:url value='/'/>">처음으로</a></p>
</body>
</html>

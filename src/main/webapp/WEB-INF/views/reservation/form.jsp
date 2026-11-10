<%-- 예약 신청 화면. ReservationController 가 "reservation/form" 을 반환하면 이 파일로 forward 된다.
     Spring 폼 태그(form:)를 쓴다:
       - form:form 은 Spring Security 의 CSRF 토큰 hidden input 을 자동으로 넣어준다
       - form:input / form:errors 는 값을 기본으로 HTML 이스케이프한다
       - 검증 실패로 돌아왔을 때 입력했던 값이 그대로 채워진다 --%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>예약 신청 - 회의실 예약</title>
    <style>
        body { font-family: sans-serif; max-width: 400px; margin: 40px auto; }
        label { display: block; margin-top: 12px; }
        input, select { width: 100%; padding: 6px; box-sizing: border-box; }
        .error { color: #c00; font-size: 0.9em; }
        .hint { color: #666; font-size: 0.9em; }
        button { margin-top: 16px; padding: 8px 16px; }
    </style>
</head>
<body>
<h1>예약 신청</h1>

<%-- 서비스가 거부한 이유(겹침, 비활성 회의실, 시간 규칙) --%>
<c:if test="${not empty errorMessage}">
    <p class="error"><c:out value="${errorMessage}"/></p>
</c:if>

<c:url var="actionUrl" value="/reservations"/>
<form:form modelAttribute="form" action="${actionUrl}" method="post">

    <label for="roomId">회의실</label>
    <form:select path="roomId">
        <form:option value="" label="-- 선택 --"/>
        <%-- items 의 각 Room 에서 id 를 값으로, name 을 표시 이름으로 쓴다 --%>
        <form:options items="${rooms}" itemValue="id" itemLabel="name"/>
    </form:select>
    <form:errors path="roomId" cssClass="error" element="div"/>

    <label for="date">날짜</label>
    <form:input path="date" type="date"/>
    <form:errors path="date" cssClass="error" element="div"/>

    <label for="startTime">시작 시각</label>
    <form:input path="startTime" type="time" step="1800"/>
    <form:errors path="startTime" cssClass="error" element="div"/>

    <label for="endTime">종료 시각</label>
    <form:input path="endTime" type="time" step="1800"/>
    <form:errors path="endTime" cssClass="error" element="div"/>
    <p class="hint">30분 단위, 최대 4시간</p>

    <label for="purpose">사용 목적</label>
    <form:input path="purpose" maxlength="200"/>
    <form:errors path="purpose" cssClass="error" element="div"/>

    <button type="submit">신청하기</button>
</form:form>

<p><a href="<c:url value='/rooms'/>">회의실 목록</a> | <a href="<c:url value='/reservations/my'/>">내 예약</a></p>
</body>
</html>

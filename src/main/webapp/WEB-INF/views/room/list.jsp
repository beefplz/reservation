<%-- 회의실 목록 (사용자용). RoomController 가 "room/list" 를 반환하면 이 파일로 forward 된다.
     규칙: 스크립틀릿(<% %>) 없이 EL(${...})과 JSTL 만 쓴다. --%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <title>회의실 목록 - 회의실 예약</title>
    <style>
        body { font-family: sans-serif; max-width: 700px; margin: 40px auto; }
        table { width: 100%; border-collapse: collapse; }
        th, td { border-bottom: 1px solid #ddd; padding: 8px; text-align: left; }
    </style>
</head>
<body>
<h1>회의실 목록</h1>

<table>
    <thead>
    <tr><th>이름</th><th>위치</th><th>수용 인원</th><th>사용 여부</th></tr>
    </thead>
    <tbody>
    <%-- rooms: 컨트롤러가 model.addAttribute("rooms", ...) 로 넣은 List<Room> --%>
    <c:forEach var="room" items="${rooms}">
        <tr>
            <%-- 이름/위치는 관리자가 입력한 값 → <c:out> 으로 HTML 이스케이프 (XSS 방지) --%>
            <td><c:out value="${room.name}"/></td>
            <td><c:out value="${room.location}"/></td>
            <td><c:out value="${room.capacity}"/>명</td>
            <%-- room.active → Room.isActive() 호출 (boolean 은 is 로 시작하는 getter) --%>
            <td>${room.active ? '사용' : '미사용'}</td>
        </tr>
    </c:forEach>
    <c:if test="${empty rooms}">
        <tr><td colspan="4">이용할 수 있는 회의실이 없습니다.</td></tr>
    </c:if>
    </tbody>
</table>

<%-- c:url 은 컨텍스트 경로(외부 Tomcat 배포 시 /reservation 등)를 자동으로 붙여준다 --%>
<p><a href="<c:url value='/'/>">처음으로</a></p>
</body>
</html>

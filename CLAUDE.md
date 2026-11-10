# 회의실 예약 시스템

취업 포트폴리오용 신입 백엔드 프로젝트. 목표는 MyBatis 기반 일반 웹서비스 경험(회원·권한·트랜잭션)을 보여주는 것.

## 기술 스택
- Java 17, Spring Boot, Gradle
- MyBatis (XML 매퍼: src/main/resources/mapper/), MySQL 8 (Docker 컨테이너 reservation-db, localhost:3306)
- Spring Security (세션 로그인, USER / ADMIN 권한), Validation, Lombok
- 뷰: Thymeleaf(src/main/resources/templates/) + JSP/JSTL(src/main/webapp/WEB-INF/views/) 공존
  - JSP 화면: 회의실 목록(/rooms). JSP 뷰 이름은 application.yml의 spring.thymeleaf.excluded-view-names에 추가할 것
  - JSP에서는 스크립틀릿 금지, EL + JSTL만 사용. 사용자 입력 값은 <c:out>으로 출력
- 패키징: 실행형 war (내장 Tomcat은 jar 안의 JSP를 지원하지 않음). 실행은 java -jar build/libs/*.war
- DB 비밀번호는 환경변수 DB_PASSWORD. 코드나 설정 파일에 비밀번호를 쓰지 말 것

## 테이블
- member(id, login_id, password[BCrypt], name, role, created_at)
- room(id, name, location, capacity, active, created_at)
- reservation(id, member_id, room_id, start_time, end_time, purpose, status, created_at, updated_at)
  - status: PENDING / APPROVED / REJECTED / CANCELED
- 스키마는 src/main/resources/schema.sql. 스키마를 바꾸면 이 파일도 함께 수정

## 핵심 규칙
- 중복 예약 방지: 같은 회의실에서 `start_time < 새 end_time AND end_time > 새 start_time`이고 status가 PENDING/APPROVED인 예약이 있으면 거부
- 동시 신청 대비: 예약 생성은 @Transactional 안에서 room 행을 SELECT ... FOR UPDATE로 잠근 뒤 검사
- 회의실은 삭제하지 않고 active = 0으로 비활성화

## 작업 방식
- 사용자가 코드를 이해하는 것이 목표. 변경할 때마다 무엇을 왜 바꿨는지 짧게 설명할 것
- 설계 판단(테이블, 트랜잭션 범위, 권한 규칙)은 바꾸기 전에 먼저 물어볼 것
- 핵심 로직(중복 예약, 권한)에는 테스트를 함께 작성
- 계층 구조: controller → service → mapper. 컨트롤러에 비즈니스 로직을 넣지 말 것
- 기능 하나가 끝나면 커밋 단위로 정리

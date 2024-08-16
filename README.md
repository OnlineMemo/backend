# OnlineMemo - Backend Refactoring
***&#8594;&nbsp;&nbsp;60x Speed Improvement***

### Project
- <a href="https://github.com/OnlineMemo">README.md</a>
- <a href="https://github.com/orgs/OnlineMemo/repositories?q=sort%3Aname-asc">FullStack Repo</a>

### Refactor
- <a href="https://github.com/OnlineMemo/backend/pull/2">Github PR</a>
- <a href="https://github.com/OnlineMemo/backend/tree/223c16c130d15a2cd024f5a1c531ad63239a13b4">Before Code</a>&nbsp;&nbsp;/&nbsp;&nbsp;<a href="https://github.com/OnlineMemo/backend/tree/refactor/onlinememo-v2">After Code</a>
- <a href="https://github.com/OnlineMemo/frontend-web/tree/refactor/onlinememo-v2">Frontend Refactor</a>

<details open>
  <summary><h3>&nbsp;Contents</h3></summary>

1. &nbsp;&nbsp;[⚙ Version](#-version)
2. &nbsp;&nbsp;[🗂️ Database](#%EF%B8%8F-database)
3. &nbsp;&nbsp;[📗 API](#-api)
4. &nbsp;&nbsp;[📈 Performance](#-performance)
   - &nbsp;&nbsp;[N+1 Query](#n1-query)
   - &nbsp;&nbsp;[Bulk Query](#bulk-query)
   - &nbsp;&nbsp;[Benchmark](#benchmark)
6. &nbsp;&nbsp;[📂 Package](#-package)
</details>
<br>



## ⚙ Version

- **Java**&nbsp;:&nbsp;&nbsp;11 &#8594; 17
- **Spring Boot**&nbsp;:&nbsp;&nbsp;2.7.8 &#8594; 3.2.7
- **Swagger**&nbsp;:&nbsp;&nbsp;Springfox &#8594; Springdoc
- **AWS CloudWatch**&nbsp;:&nbsp;&nbsp;Agent &#8594; Logs

<br>



## 🗂️ Database

### Before&nbsp;&nbsp;/&nbsp;&nbsp;After
<img width="1470" alt="before DB ERD" src="https://github.com/OnlineMemo/.github/assets/56509933/6bf90043-9bb4-435d-9ac3-5c8e8123a34c">
<img width="1470" alt="after DB ERD" src="https://github.com/user-attachments/assets/48beb98f-f616-4950-b1c5-05d779a90e0d">

#### 부적절한 컬럼명 수정
- login_id&nbsp;&nbsp;&#8594;&nbsp;&nbsp;email
- first_password&nbsp;&nbsp;&#8594;&nbsp;&nbsp;password
- username&nbsp;&nbsp;&#8594;&nbsp;&nbsp;nickname

#### 자료형 변경
- 날짜를 문자열로 DB에 저장할 경우, 추후 정렬 시 속도와 정확성 측면에서 불리함.
- LocalDateTime으로 DB에 저장 후, 응답 시 원하는 포맷의 문자열로 변환하는 방안을 채택.
- modified_date (VARCHAR)&nbsp;&nbsp;&#8594;&nbsp;&nbsp;modified_time (DATETIME)

#### Enum 사용
- is_friend : 0&nbsp;,&nbsp;&nbsp;is_wait : 1&nbsp;&nbsp;&#8594;&nbsp;&nbsp;friendship_state (SEND)
- is_friend : 1&nbsp;,&nbsp;&nbsp;is_wait : 0&nbsp;&nbsp;&#8594;&nbsp;&nbsp;friendship_state (FRIEND)

#### sender_user_id 실제 매핑
- 기존 id 값을 직접 저장하는 방식은, 추후 조회 시 추가적인 쿼리 및 메소드 호출을 동반함.
- Friendship 테이블에 User 테이블을 두 번 연관관계 매핑하여, senderUser도 연결하는 방안을 채택.
- sender_user_id (Long)&nbsp;&nbsp;&#8594;&nbsp;&nbsp;sender_user_id (User)

#### refresh_token 컬럼 추가
- JWT Access Token만 운용 시, 6시간의 짧은 로그인 유지시간을 가지며 보안에 취약함.
- Access Token 만료 시, Refresh Token으로 재발급 받아 2주동안 로그인 유지가 가능하며 보안이 강화됨.
- Access Token&nbsp;&nbsp;&#8594;&nbsp;&nbsp;Access Token + Refresh Token 함께 운용.&nbsp;&nbsp;(FE : Axios Interceptor 적용)

<br>



## 📗 API

**<a href="https://github.com/user-attachments/assets/128c819e-2424-487d-aac0-23611d68af1c">Before</a>**|**<a href="https://github.com/user-attachments/assets/4b60a166-ff46-4a0e-a14e-20bb2722273b">After</a>**
|:-----:|:-----:|
<img src="https://github.com/user-attachments/assets/128c819e-2424-487d-aac0-23611d68af1c" width="100%">|<img src="https://github.com/user-attachments/assets/4b60a166-ff46-4a0e-a14e-20bb2722273b" width="100%">
| -&nbsp;&nbsp;불필요하게 많은 API 호출로 성능 저하 발생<br> -&nbsp;&nbsp;사용자에게 userId가 자주 노출되어 보안성 저하| -&nbsp;&nbsp;RestFul URI 및 API 개수 단축으로 성능 향상<br> -&nbsp;&nbsp;Security Context 정보로 userId를 대체하여 보안성 향상|

<br>



## 📈 Performance

### N+1 Query

<details>
  <summary>&nbsp;<strong>Code</strong>&nbsp;:&nbsp;Open!</summary>

#### Repository
```java
// < Before - JPA 쿼리 메소드 (Lazy 조회) >
Optional<User> findById(Long userId);              // User

// < After - Fetch Join 메소드 (Eager 조회) >
@Query("SELECT u FROM User u " +                   // User
        "LEFT JOIN FETCH u.userMemoList uml " +    // + User.userMemoList
        "LEFT JOIN FETCH uml.memo m " +            // + User.userMemoList.memo
        "LEFT JOIN FETCH m.userMemoList umll " +   // + User.userMemoList.memo.userMemoList
        "LEFT JOIN FETCH umll.user " +             // + User.userMemoList.memo.userMemoList.user
        "WHERE u.id = :userId")
Optional<User> findByIdToDeepUserWithEager(@Param("userId") Long userId);
```

#### Service
```java
@Transactional(readOnly = true)
@Override
public List<MemoDto.MemoPageResponse> findMemos(String filter, String search) {  // 메모 목록 조회,정렬,검색 로직
    if(filter != null && search != null) throw new Exception400.MemoBadRequest("잘못된 쿼리파라미터로 API를 요청하였습니다.");
    Predicate<Memo> memoPredicate = (filter != null) ? filterMemos(filter) : searchMemos(search);
    Long loginUserId = SecurityUtil.getCurrentMemberId();

    // < Before - JPA 쿼리 메소드 (Lazy 조회) >  N+1 문제 O
    User user = userRepository.findById(loginUserId).orElseThrow(() -> new Exception404.NoSuchUser(String.format("userId = %d", loginUserId)));

    // < After - Fetch Join 메소드 (Eager 조회) >  N+1 문제 X
    User user = userRepository.findByIdToDeepUserWithEager(loginUserId).orElseThrow(() -> new Exception404.NoSuchUser(String.format("userId = %d", loginUserId)));

    List<MemoDto.MemoPageResponse> memoPageResponseDtoList = user.getUserMemoList().stream()
            .map(UserMemo::getMemo)              // User.userMemoList (N+1 쿼리 발생)
            .filter(memoPredicate)               // User.userMemoList.memo (N+1 쿼리 발생)
            .sorted(Comparator.comparing(Memo::getModifiedTime, Comparator.reverseOrder())
                    .thenComparing(Memo::getId, Comparator.reverseOrder()))
            .map(MemoDto.MemoPageResponse::new)  // User.userMemoList.memo.userMemoList & User.userMemoList.memo.userMemoList.user (내부에서 N+1 쿼리 발생)
            .collect(Collectors.toList());

    return memoPageResponseDtoList;
}
```
</details>

**Before<br>(JPA method)**|**After<br>(JPQL Fetch Join)**
|:-----:|:-----:|
<img src="https://github.com/user-attachments/assets/6e8eb39e-4439-4b58-b0f0-a1f29142284a" height="600px">|<img src="https://github.com/user-attachments/assets/450451b2-c04b-47f4-b60f-36fc90084b80" height="600px">
| -&nbsp;&nbsp;JPA 쿼리 메소드로 상위 엔티티를 조회한 경우<br> -&nbsp;&nbsp;하위 엔티티에 접근시 추가적인 N+1 쿼리 발생<br> -&nbsp;&nbsp;잦은 DB 접근으로 성능 저하 발생| -&nbsp;&nbsp;Fetch Join을 활용한 JPQL 메소드로 상위 엔티티를 조회한 경우<br> -&nbsp;&nbsp;지정한 하위 엔티티까지 Eager 조회하여 N+1 문제 해결<br> -&nbsp;&nbsp;쿼리 개선으로 DB 접근을 최소화하여 성능 최적화|

### Bulk Query

<details>
  <summary>&nbsp;<strong>Code</strong>&nbsp;:&nbsp;Open!</summary>

#### Repository
```java
// < Before - JPA saveAll >
void saveAll(List<UserMemo> userMemoList);

// < Before - JPA deleteAll >
void deleteAll(List<Memo> memoList);  // deleteAllInBatch()는 OR절의 성능 저하와 오버헤드의 가능성으로 사용하지 않았음.

// < After - JDBC Batch Insert >
public void batchInsert(List<UserMemo> userMemoList) {
    String sql = "INSERT INTO user_memo (user_id, memo_id) VALUES (?, ?)";

    for (int i=0; i<userMemoList.size(); i+=BATCH_SIZE) {  // 'BATCH_SIZE = 1000' 배치 크기 설정 (메모리 오버헤드 방지)
        List<UserMemo> batchList = userMemoList.subList(i, Math.min(i+BATCH_SIZE, userMemoList.size()));

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                UserMemo userMemo = batchList.get(i);
                ps.setLong(1, userMemo.getUser().getId());
                ps.setLong(2, userMemo.getMemo().getId());
            }

            @Override
            public int getBatchSize() {
                return batchList.size();
            }
        });
    }
}

// < After - JDBC Batch Delete >
public void batchDelete(List<Memo> memoList) {

    for (int i=0; i<memoList.size(); i+=BATCH_SIZE) {
        List<Long> batchList = memoList.subList(i, Math.min(i+BATCH_SIZE, memoList.size()))
                .stream()
                .map(Memo::getId)
                .collect(Collectors.toList());

        String sql = String.format("DELETE FROM memo WHERE memo_id IN (%s)",  // OR절이 아닌 IN절 사용.
                batchList.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(",")));

        jdbcTemplate.update(sql);
    }
}
```
</details>

**Before&nbsp;&nbsp;&#8594;&nbsp;&nbsp;After<br>(JPA saveAll)&nbsp;&nbsp;&#8594;&nbsp;&nbsp;(JDBC Batch Insert)**|**Before&nbsp;&nbsp;&#8594;&nbsp;&nbsp;After<br>(JPA deleteAll)&nbsp;&nbsp;&#8594;&nbsp;&nbsp;(JDBC Batch Delete + IN절)**
|-----|-----|
<img src="https://github.com/user-attachments/assets/c340175a-05c0-417b-9ab1-4f41a811f4a8" height="600px">|<img src="https://github.com/user-attachments/assets/7ebee676-e8ae-41ab-8ed7-522801197c7a" height="600px">
|-&nbsp;&nbsp;메모에 여러 친구를 초대할 때 Bulk Insert 필요<br> -&nbsp;&nbsp;id전략이 IDENTITY라 saveAll이 벌크로 동작하지 않음<br> -&nbsp;&nbsp;JPA saveAll 대신 JDBC Batch Insert 사용<br> -&nbsp;&nbsp;10명의 친구 초대 시 쿼리가 10회에서 1회로 감소|-&nbsp;&nbsp;회원 탈퇴 시 모든 메모를 Bulk Delete 필요<br> -&nbsp;&nbsp;JPA의 내부 순회로 deleteAll이 벌크로 동작하지 않음<br> -&nbsp;&nbsp;JPA deleteAll 대신 JDBC Batch Delete 사용<br> -&nbsp;&nbsp;OR 대신 IN 절로 오버헤드와 성능 이점 확보<br> -&nbsp;&nbsp;10개의 메모 삭제 시 쿼리가 10회에서 1회로 감소|

### Benchmark

**Before<br>(MemoPage - 30 memos)**|**After<br>(MemoPage - 30 memos)**
|-----|-----|
<img src="https://github.com/user-attachments/assets/72d75f87-f0a9-4860-bffc-eba280c949da" width="100%">|<img src="https://github.com/user-attachments/assets/85f2d5a2-574a-4046-82b1-a42c9cfff2c5" width="100%">
| -&nbsp;&nbsp;FE : 각각의 모든 하위 컴포넌트에서 API 다중 호출<br> -&nbsp;&nbsp;Result&nbsp;:&nbsp;&nbsp;Request = 91번&nbsp;&nbsp;&&nbsp;&nbsp;Finish Time = 11.27s| -&nbsp;&nbsp;FE : 상위 컴포넌트에서 API 호출 후 하위로 props 전달<br>-&nbsp;&nbsp;BE : 전체적인 비즈니스 로직 및 쿼리 개선<br> -&nbsp;&nbsp;Result&nbsp;:&nbsp;&nbsp;Request = 2번&nbsp;&nbsp;&&nbsp;&nbsp;Finish Time = 193ms<br><br> &#8594;&nbsp;&nbsp;불과 30개의 메모임에도, 무려 58.4배의 성능 개선<br> &#8594;&nbsp;&nbsp;Prod 서버 재배포 시, 최소 60배 이상의 속도 향상 예상

<br>



## 📂 Package

### Domain&nbsp;&nbsp;&&nbsp;&nbsp;Repository
**Before**|**After**
|:-----:|:-----:|
<img src="https://github.com/user-attachments/assets/35bc3d24-d21e-4f6a-9a18-5724116f594b" width="100%">|<img src="https://github.com/user-attachments/assets/8b84c857-ae7a-4f0a-befe-3ae0c7c16fbb" width="75%">
| -&nbsp;&nbsp;Entity와 Repository의 패키징 혼용<br> -&nbsp;&nbsp;Entity명과 동일한 상위 디렉토리 할당| -&nbsp;&nbsp;Entity와 Repository의 패키징 분리<br> -&nbsp;&nbsp;역할에 따른 Entity 상위 디렉토리 할당|

### DTO
**Before**|**After**
|:-----:|:-----:|
<img src="https://github.com/user-attachments/assets/4e8486a9-1112-43b9-82bc-65316e9a2e4b" width="70%">|<img src="https://github.com/user-attachments/assets/e21452bd-81f7-41bc-b919-2af7d4f39d9d" width="100%">
| -&nbsp;&nbsp;잘못된 도메인별 DTO 분배<br> -&nbsp;&nbsp;동일 디렉토리 내 요청&응답 DTO 혼용<br> -&nbsp;&nbsp;무분별한 네이밍으로 복잡성 증가| -&nbsp;&nbsp;Inner Class를 활용한 DTO 분리<br> -&nbsp;&nbsp;동일 클래스 내 static DTO 네이밍 규칙 준수|

### Exception
**Before**|**After**
|:-----:|:-----:|
<img src="https://github.com/user-attachments/assets/15add028-4e1f-4faf-8ab6-01cd2a75da99" width="74%">|<img src="https://github.com/user-attachments/assets/25dc7949-15aa-4b50-97c0-01bdc3a87c6d" width="100%">
| -&nbsp;&nbsp;역할 없는 무분별한 Exception 생성<br> -&nbsp;&nbsp;Handler에 예외처리 응답을 일일이 작성| -&nbsp;&nbsp;추상화 CustomException 클래스 상속<br> -&nbsp;&nbsp;inner 방식으로 static Exception 생성<br> -&nbsp;&nbsp;Handler는 400,404,500 클래스만 타겟팅|

### Directory Structure

```
< Before >                                        < After >
----------------------------------------------------------------------------------------------
:                                                 :
├── config                                        ├── config
│   ├── JwtSecurityConfig.java                    │   ├── SecurityConfig.java
│   ├── SwaggerConfig.java                        │   └── SwaggerConfig.java
│   └── WebSecurityConfig.java                    ├── controller
├── controller                                    │   ├── AuthController.java
│   ├── AuthController.java                       │   ├── FriendshipController.java
│   ├── FriendshipController.java                 │   ├── MemoController.java
│   ├── MemoController.java                       │   ├── TestController.java
│   ├── TestController.java                       │   └── UserController.java
│   └── UserController.java                       ├── domain
├── domain                                        │   ├── Friendship.java
│   ├── DefaultFriendshipEntity.java              │   ├── Memo.java
│   ├── DefaultMemoEntity.java                    │   ├── User.java
│   ├── friendship                                │   ├── common
│   │   ├── Friendship.java                       │   │   └── BaseEntity.java
│   │   └── FriendshipJpaRepository.java          │   ├── enums
│   ├── memo                                      │   │   ├── Authority.java
│   │   ├── Memo.java                             │   │   └── FriendshipState.java
│   │   └── MemoJpaRepository.java                │   └── mapping
│   ├── user                                      │       └── UserMemo.java
│   │   ├── Authority.java                        ├── dto
│   │   ├── User.java                             │   ├── AuthDto.java
│   │   └── UserJpaRepository.java                │   ├── FriendshipDto.java
│   └── userandmemo                               │   ├── MemoDto.java
│       ├── UserAndMemo.java                      │   └── UserDto.java
│       └── UserAndMemoJpaRepository.java         ├── jwt
├── dto                                           │   ├── CustomUserDetailsService.java
│   ├── friendship                                │   ├── JwtFilter.java
│   │   ├── FriendshipRequestDto.java             │   ├── TokenProvider.java
│   │   ├── FriendshipResponseDto.java            │   └── handler
│   │   ├── FriendshipSendRequestDto.java         │       ├── JwtAccessDeniedHandler.java
│   │   ├── FriendshipSendResponseDto.java        │       ├── JwtAuthenticationEntryPoint.java
│   │   └── FriendshipUpdateRequestDto.java       │       └── JwtExceptionFilter.java
│   ├── memo                                      ├── repository
│   │   ├── MemoInviteResponseDto.java            │   ├── FriendshipBatchRepository.java
│   │   ├── MemoResponseDto.java                  │   ├── FriendshipRepository.java
│   │   ├── MemoSaveRequestDto.java               │   ├── MemoBatchRepository.java
│   │   ├── MemoSaveResponseDto.java              │   ├── MemoRepository.java
│   │   ├── MemoUpdateRequestDto.java             │   ├── UserMemoBatchRepository.java
│   │   └── MemoUpdateStarRequestDto.java         │   ├── UserMemoRepository.java
│   ├── token                                     │   └── UserRepository.java
│   │   └── TokenDto.java                         ├── response
│   ├── user                                      │   ├── GlobalExceptionHandler.java
│   │   ├── UserIdResponseDto.java                │   ├── ResponseCode.java
│   │   ├── UserLoginRequestDto.java              │   ├── ResponseData.java
│   │   ├── UserRequestDto.java                   │   ├── exception
│   │   ├── UserRequestDtos.java                  │   │   ├── CustomException.java
│   │   ├── UserResponseDto.java                  │   │   ├── Exception400.java
│   │   ├── UserSignupRequestDto.java             │   │   ├── Exception404.java
│   │   ├── UserUpdateNameRequestDto.java         │   │   └── Exception500.java
│   │   └── UserUpdatePwRequestDto.java           │   └── responseitem
│   └── userandmemo                               │       ├── MessageItem.java
│       ├── UserAndMemoRequestDto.java            │       └── StatusItem.java
│       └── UserAndMemoResponseDto.java           ├── service
├── jwt                                           │   ├── AuthService.java
│   ├── JwtAccessDeniedHandler.java               │   ├── FriendshipService.java
│   ├── JwtAuthenticationEntryPoint.java          │   ├── MemoService.java
│   ├── JwtFilter.java                            │   ├── UserMemoService.java
│   └── TokenProvider.java                        │   ├── UserService.java
├── response                                      │   └── impl
│   ├── GlobalExceptionHandler.java               │       ├── AuthServiceImpl.java
│   ├── ResponseCode.java                         │       ├── FriendshipServiceImpl.java
│   ├── ResponseData.java                         │       ├── MemoServiceImpl.java
│   ├── exception                                 │       ├── UserMemoServiceImpl.java
│   │   ├── FriendshipBadRequestException.java    │       └── UserServiceImpl.java
│   │   ├── FriendshipDuplicateException.java     └── util
│   │   ├── LoginIdDuplicateException.java            ├── SecurityUtil.java
│   │   ├── MemoSortBadRequestException.java          └── TimeConverter.java
│   │   ├── NoSuchFriendshipException.java
│   │   ├── NoSuchMemoException.java
│   │   ├── NoSuchUserException.java
│   │   └── UserAndMemoDuplicateException.java
│   └── responseitem
│       ├── MessageItem.java
│       └── StatusItem.java
├── service
│   ├── FriendshipService.java
│   ├── MemoService.java
│   ├── UserAndMemoService.java
│   ├── UserService.java
│   ├── auth
│   │   ├── AuthService.java
│   │   └── CustomUserDetailsService.java
│   └── logic
│       ├── FriendshipServiceLogic.java
│       ├── MemoServiceLogic.java
│       ├── UserAndMemoServiceLogic.java
│       └── UserServiceLogic.java
└── util
    └── SecurityUtil.java
```

<br>

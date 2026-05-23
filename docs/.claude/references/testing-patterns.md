# Testing Patterns

## Unit Test Example

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void findByIdReturnsUserWhenFound() {
        // arrange
        // act
        // assert
    }
}
```

## Controller Test Example

```java
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;
}
```

## Repository Test Example

```java
@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;
}
```

## Commands

```bash
./mvnw test
./mvnw clean test
```

Windows:

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean test
```

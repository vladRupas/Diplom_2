import com.github.javafaker.Faker;
import com.google.gson.Gson;
import io.qameta.allure.Description;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class UserLoginTest {

    private final Gson gson = new Gson();
    private final Faker faker = new Faker();

    private String email;
    private String password;
    private String name;
    private String accessToken = null;

    @Before
    public void setup() {
        RestAssured.baseURI = "https://stellarburgers.nomoreparties.site";
    }

    @Test
    @DisplayName("User registration and login test")
    @Description("Create user, login with correct and incorrect credentials, verify responses")
    public void userRegistrationAndLogin() {

        generateTestData();
        createUser();

        // логин под существующим пользователем
        Login correctLogin = new Login(email, password);
        Response loginResponse = loginUser(correctLogin);
        loginResponse.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("user.email", equalTo(email))
                .body("user.name", equalTo(name))
                .body("accessToken", startsWith("Bearer "))
                .body("refreshToken", notNullValue());

        accessToken = loginResponse.path("accessToken");

        // логин с неверным логином
        Login wrongEmailLogin = new Login("wrong" + email, password);
        loginUser(wrongEmailLogin)
                .then()
                .statusCode(401)
                .body("success", equalTo(false))
                .body("message", equalTo("email or password are incorrect"));

        // логин с неверным паролем
        Login wrongPasswordLogin = new Login(email, "wrongPassword");
        loginUser(wrongPasswordLogin)
                .then()
                .statusCode(401)
                .body("success", equalTo(false))
                .body("message", equalTo("email or password are incorrect"));
    }

    @Step("Generate test user data")
    private void generateTestData() {
        email = faker.internet().emailAddress();
        password = faker.internet().password(8, 12);
        name = faker.name().firstName();
    }

    @Step("Create user")
    private void createUser() {
        NewUser newUser = new NewUser(email, password, name);
        String json = gson.toJson(newUser);

        given()
                .header("Content-type", "application/json")
                .body(json)
                .post("/api/auth/register");
    }

    @Step("Login user")
    private Response loginUser(Login credentials) {
        return given()
                .header("Content-type", "application/json")
                .body(gson.toJson(credentials))
                .post("/api/auth/login");
    }

    @Step("Deleting user by accessToken")
    private void deleteUserByToken(String token) {
        given()
                .header("Authorization", token)
                .delete("/api/auth/user")
                .then()
                .statusCode(202);  // or 200 if your API returns that, adjust accordingly
    }

    @After
    public void cleanup() {
        if (accessToken != null) {
            deleteUserByToken(accessToken);
        }
    }

}
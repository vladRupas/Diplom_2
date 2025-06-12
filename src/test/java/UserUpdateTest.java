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
import static org.hamcrest.Matchers.equalTo;

public class UserUpdateTest {

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
    @DisplayName("Update user data with and without authorization")
    @Description("Verifies successful update of user data when authorized, and failure when unauthorized")
    public void updateUserDataTest() {

        generateTestData();
        registerUser();
        accessToken = loginAndGetAccessToken(email, password);

        String updatedEmail = faker.internet().emailAddress();
        String updatedName = faker.name().firstName();

        updateUserWithAuth(updatedEmail, updatedName)
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("user.email", equalTo(updatedEmail.toLowerCase()))
                .body("user.name", equalTo(updatedName));

        updateUserWithoutAuth(updatedEmail, updatedName)
                .then()
                .statusCode(401)
                .body("success", equalTo(false))
                .body("message", equalTo("You should be authorised"));
    }

    @Step("Generate test user data")
    private void generateTestData() {
        email = faker.internet().emailAddress();
        password = faker.internet().password();
        name = faker.name().firstName();
    }

    @Step("Register a new user")
    private void registerUser() {
        NewUser user = new NewUser(email, password, name);
        given()
                .header("Content-type", "application/json")
                .body(gson.toJson(user))
                .post("/api/auth/register");
    }

    @Step("Login user and retrieve access token")
    private String loginAndGetAccessToken(String email, String password) {
        Login login = new Login(email, password);
        Response response = given()
                .header("Content-type", "application/json")
                .body(gson.toJson(login))
                .post("/api/auth/login");

        return response.then()
                .statusCode(200)
                .extract()
                .path("accessToken");
    }

    @Step("Update user data with auth")
    private Response updateUserWithAuth(String newEmail, String newName) {
        return given()
                .header("Content-type", "application/json")
                .header("Authorization", accessToken)
                .body(String.format("{\"email\":\"%s\",\"name\":\"%s\"}", newEmail, newName))
                .patch("/api/auth/user");
    }

    @Step("Update user data without auth")
    private Response updateUserWithoutAuth(String newEmail, String newName) {
        return given()
                .header("Content-type", "application/json")
                .body(String.format("{\"email\":\"%s\",\"name\":\"%s\"}", newEmail, newName))
                .patch("/api/auth/user");
    }

    @Step("Deleting user by accessToken")
    private void deleteUserByToken(String token) {
        given()
                .header("Authorization", token)
                .delete("/api/auth/user")
                .then()
                .statusCode(202);
    }

    @After
    public void cleanup() {
        if (accessToken != null) {
            deleteUserByToken(accessToken);
        }
    }
}
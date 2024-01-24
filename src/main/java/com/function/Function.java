package com.function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    /**
     * This function listens at endpoint "/api/HttpExample". Two ways to invoke it
     * using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpExample
     * 2. curl "{your host}/api/HttpExample?name=HTTP%20Query"
     */
    @FunctionName("HttpExample")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = { HttpMethod.GET,
                    HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<BikeModel>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");
        // Extract bike model from the request
        BikeModel bikeModel = request.getBody().get();

        // Get your database connection details
        String jdbcurl = "jdbc:h2:mem:test";
        String username = "sa";
        String password = "";

        try (Connection connection = DriverManager.getConnection(jdbcurl, username, password)) {
            createBikesTable(connection);
            // Insert data into the "Bikes" table
            String sql = "INSERT INTO Bikes (Id, Brand, Model, EngineCapacity, EngineName) VALUES (?, ?, ?, ?, ?)";

            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setInt(1, bikeModel.getId());
                preparedStatement.setString(2, bikeModel.getBrand());
                preparedStatement.setString(3, bikeModel.getModel());
                preparedStatement.setInt(4, bikeModel.getEngineCapacity());
                preparedStatement.setString(5, bikeModel.getEngineName());

                preparedStatement.executeUpdate();
            }

            context.getLogger().info("Data inserted successfully.");
        } catch (SQLException e) {
            context.getLogger().severe("SQL Exception: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing the request.")
                    .build();
        }

        return request.createResponseBuilder(HttpStatus.OK)
                .body("Data inserted successfully.")
                .build();
    }

    private void createBikesTable(Connection connection) throws SQLException {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS Bikes (Id INT PRIMARY KEY, Brand VARCHAR(255), Model VARCHAR(255), EngineCapacity INT, EngineName VARCHAR(255))";

        try (PreparedStatement preparedStatement = connection.prepareStatement(createTableSQL)) {
            preparedStatement.executeUpdate();
        }

    }
}

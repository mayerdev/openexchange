package router

import (
	"openexchange/auth/controllers/auth"

	"github.com/gofiber/fiber/v3"
)

func Setup(app *fiber.App) {
	authGroup := app.Group("/auth")
	authGroup.Post("/login", auth.Login)
}

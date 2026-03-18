package utils

import (
	"testing"

	"github.com/go-playground/validator/v10"
)

func TestPasswordValidate(t *testing.T) {
	v := validator.New()

	_ = v.RegisterValidation("password", PasswordValidate)

	tests := []struct {
		name     string
		password string
		want     bool
	}{
		{"Valid password", "Password123!", true},
		{"Too short", "Pas1!", false},
		{"No uppercase", "password123!", false},
		{"No lowercase", "PASSWORD123!", false},
		{"No number", "Password!", false},
		{"No special", "Password123", false},
		{"Empty", "", false},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			type TestStruct struct {
				Password string `validate:"password"`
			}

			s := TestStruct{Password: tt.password}
			err := v.Struct(s)

			if (err == nil) != tt.want {
				t.Errorf("PasswordValidate(%s) = %v, want %v", tt.password, err == nil, tt.want)
			}
		})
	}
}

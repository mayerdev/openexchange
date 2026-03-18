package models

import "github.com/google/uuid"

type TraderCredential struct {
	BaseModel
	TraderID         uuid.UUID `json:"trader_id"`
	Type             string    `json:"type"`
	Value            string    `json:"value"`
	VerificationCode string    `json:"verification_code"`
	Verified         bool      `json:"verified" gorm:"default:false"`
}

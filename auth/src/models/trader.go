package models

type Trader struct {
	BaseModel
	Password string `json:"password"`
	Status   string `json:"status" gorm:"default:active"`
}

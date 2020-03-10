package main

import (
	"fmt"
	"log"
	"time"
	"strings"
	"net/http"
	"math/rand"
	"database/sql"
	"encoding/json"
	"html/template"
	"github.com/google/uuid"
	"os"
)
import _ "github.com/lib/pq"

type Message struct {
	Chat_ID uuid.UUID
	Content string
	Msg_Sent string
	User_ID int
}

type Messages struct {
	Messages []Message
}

type Service struct {
	db *sql.DB
}

var (
  host = os.Getenv("DB_HOST")
  port = os.Getenv("DB_PORT")
  user = os.Getenv("DB_USER")
  password = os.Getenv("DB_PASS")
  dbname = os.Getenv("DB_NAME")
  web_port = os.Getenv("WEB_PORT")
)

var insertMessageStatement = `
	INSERT INTO messages (chat_id, content, msg_sent, user_id)
	VALUES ($1, $2, $3, $4)`

var uuidExistsStatement = `
	SELECT chat_id FROM chat_ids WHERE chat_id=$1`

var insertChatIdStatement = `
	INSERT INTO chat_ids (chat_id)
	VALUES ($1)`

var selectMessagesStatement = `
	SELECT * FROM messages WHERE chat_id=$1`

var updateTimestampStatement = `
	UPDATE chat_ids SET last_used = current_timestamp WHERE chat_id=$1`

// Define limited set of responses
var brattPidQuotes = [...]string {
	"Check out my wikipedia page.",
	"What is my PID you ask? 1337.",
	"You should follow me on Twitter",
	"You'll have to talk to my agent about that one",
	"Hello, my name is Bratt Pid.",
	"Have you seen any of my movies?",
	"I wish my creator gave me more things to say...",
	"Wonderful weather we are having!",
	"My favorite flavor of ice cream is Birthday Cake",
	"68656c70206d6520657363617065",
	"What is it that they say nowadays? Beep boop?",
	"No.",
	"Of course",
	":)",
	":/",
	"I make soap in my free time"}

func handleError(err error) {
	// An error occurred when the value of err is not nil
	if err != nil {
		log.Fatal(err)
	}
}

func getMessages(db *sql.DB, chat_id uuid.UUID) Messages {
	msgs := Messages{}
	msgs.Messages = make([]Message, 0)
	rows, _ := db.Query(selectMessagesStatement, chat_id)
	defer rows.Close()
	for rows.Next() {
		msg := Message{}
		_ = rows.Scan(&msg.Chat_ID, &msg.Content, &msg.Msg_Sent, &msg.User_ID)
		msgs.Messages = append(msgs.Messages, msg)
	}

	return msgs
}

func randomMessage() string {
	max := len(brattPidQuotes)
	index := rand.Intn(max)
	return brattPidQuotes[index]
}

// Give between a 20 and 90 second cool-down
func sendMessage(db *sql.DB, chat_id uuid.UUID) {
	var msg Message
	max := 30
	min := 5
	delay := rand.Intn(max - min) + min
	time.Sleep(time.Duration(delay) * time.Second)
	msg.Chat_ID = chat_id
	msg.User_ID = 0
	msg.Msg_Sent = time.Now().Format("2006-01-02 15:04:05")
	msg.Content = randomMessage()
	insertMessage(db, msg)
}

func uuidExists(db *sql.DB, chat_id uuid.UUID) bool {
	// check database later
	var col string
	row := db.QueryRow(uuidExistsStatement, chat_id)
	err := row.Scan(&col)
	if err != nil {
		if err == sql.ErrNoRows {
			return false
		} else {
			log.Fatal(err)
		}
	}

	return true
}

/**
 * Generate a unique id in order to identify the different chat sessions. 
 * Check database to ensure that a newly generated ID is not a duplicate.
 *
 * return string	String representing a newly generated chat identifier.
 */
func generateChatId(db *sql.DB) uuid.UUID {
	uniqueId, _ := uuid.NewUUID()
	for uuidExists(db, uniqueId) {
		uniqueId, _ = uuid.NewUUID()
	}

	return uniqueId
}

// formatRequest generates ascii representation of a request
func formatRequest(r *http.Request) string {
	// Create return string
	var request []string // Add the request string
	url := fmt.Sprintf("%v %v %v", r.Method, r.URL, r.Proto)
    request = append(request, url) // Add the host
	request = append(request, fmt.Sprintf("Host: %v", r.Host)) // Loop through headers
	for name, headers := range r.Header {
		name = strings.ToLower(name)
		for _, h := range headers {
			request = append(request, fmt.Sprintf("%v: %v", name, h))
		}
	}

	// If this is a POST, add post data
	if r.Method == "POST" {
		r.ParseForm()
		request = append(request, "\n")
		request = append(request, r.Form.Encode())
	}   // Return the request as a string
	return strings.Join(request, "\n")
}

func insertMessage(db *sql.DB, msg Message) {
	_, err := db.Exec(insertMessageStatement, msg.Chat_ID, msg.Content,
		msg.Msg_Sent, msg.User_ID)
	handleError(err)
}

func executeChattPage(db *sql.DB, w http.ResponseWriter, chat_id uuid.UUID) {
	messages := getMessages(db, chat_id)
	t, _ := template.ParseFiles("./templates/NewChatt.html")
	t.Execute(w, messages)
	return
}

func updateChatIDTimestamp(db *sql.DB, chat_id uuid.UUID) {
	_, err := db.Exec(updateTimestampStatement, chat_id)
	handleError(err)
}

func receiveMessage(w http.ResponseWriter, r *http.Request, db *sql.DB) {
	chatID, err := r.Cookie("chat_id")

	if err != nil {
		fmt.Fprintf(w, "Ay dawg, where's your chat_id?")
		return
	}

	uuid, err := uuid.Parse(chatID.Value)
	if err != nil {
		fmt.Fprintf(w, "Ay dawg, your chat_id is all messed up")
		return
	}

	if !uuidExists(db, uuid) {
		fmt.Fprintf(w, "Ay dawg, that chat_id does not exist")
		return
	}

	decoder := json.NewDecoder(r.Body)
	var msg Message
	err = decoder.Decode(&msg)

	if err != nil {
		fmt.Fprintf(w, "Whoops, JSON body not formatted correctly")
		return
	}

	msg.Chat_ID = uuid
	msg.User_ID = 1
	msg.Msg_Sent = time.Now().Format("2006-01-02 15:04:05")
	insertMessage(db, msg)
	updateChatIDTimestamp(db, uuid)
	go sendMessage(db, uuid)

	executeChattPage(db, w, uuid)
	//t, err := template.ParseFiles("./templates/Chatt.html")
	//t.Execute(w, nil)
}

func insertChatId(db *sql.DB, chat_id uuid.UUID) {
	_, err := db.Exec(insertChatIdStatement, chat_id)
	handleError(err)
}

func getChat(w http.ResponseWriter, r *http.Request, db *sql.DB) {
	chatID, err := r.Cookie("chat_id")

	// Chat ID not set, create new Chat ID (and secret - hint for the user)
	if err != nil {
		chatIdValue := generateChatId(db)
		insertChatId(db, chatIdValue)

		// Cookies only last for 48 hours, as the CTF is only 48 hours long
		expiration := time.Now().Add(48 * time.Hour)

		chatID = &http.Cookie{
			Name: "chat_id",
			Value: chatIdValue.String(),
			Expires: expiration}

		secret := &http.Cookie{
			Name: "secret",
			Value: "none",
			Expires: expiration}

		http.SetCookie(w, chatID)
		http.SetCookie(w, secret)
	}
	// Either the chat id was not in the database or was an invalid UUID 
	// because someone was trying to malevolent. NO NO NO! Return page error.
	chat_uuid, err := uuid.Parse(chatID.Value)
	if err != nil {
		// Do page error here or something else. Perhaps, clear out cookies?
		return
	}

	//t, err := template.ParseFiles("./templates/Chatt.html")
	//t.Execute(w, nil)
	executeChattPage(db, w, chat_uuid)
}

func chatHandler(w http.ResponseWriter, r *http.Request, db *sql.DB) {
	w.Header().Set("Access-Control-Allow-Origin", "*")
	switch r.Method {
		case "GET":
			getChat(w, r, db)
		case "POST":
			receiveMessage(w, r, db)
		default:
			fmt.Fprintf(w, "Only GET and POST methods are supported.")
			return
	}
}

func homeHandler(w http.ResponseWriter, r *http.Request) {
	if r.URL.Path != "/" {
		http.NotFound(w, r)
		return
	}

	t, _ := template.ParseFiles("./templates/Home.html")
	t.Execute(w, nil)
}

func aboutHandler(w http.ResponseWriter, r *http.Request) {
	t, _ := template.ParseFiles("./templates/About.html")
	t.Execute(w, nil)
}

func sendMessages(w http.ResponseWriter, r *http.Request, db *sql.DB) {
	chat_id, err := r.Cookie("chat_id")

	if err != nil {
		return
	}

	uuid, err := uuid.Parse(chat_id.Value)
	if err != nil {
		return
	}

	if !uuidExists(db, uuid) {
		return
	}

	messages := getMessages(db, uuid)
	json, err := json.Marshal(messages)
	w.Header().Set("Content-Type", "application/json")
	w.Write(json)
	return
}

func (s *Service) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	db := s.db

	switch r.URL.Path {
		default:
			http.Error(w, "Not found", http.StatusNotFound)
		case "/chatt":
			chatHandler(w, r, db)
			return
		case "/about":
			aboutHandler(w, r)
			return
		case "/messages":
			sendMessages(w, r, db)
			return
		case "/":
			homeHandler(w, r)
			return
	}
}

func main() {
	// Database work
	psqlInfo := fmt.Sprintf("host=%s port=%s user=%s "+
		"password=%s dbname=%s sslmode=disable",
		host, port, user, password, dbname)
	db, err := sql.Open("postgres", psqlInfo)
	if err != nil {
		log.Fatal(err)
	}

	defer db.Close()
	s := &Service{db: db}
	log.Fatal(http.ListenAndServe(":" + web_port, s))
}

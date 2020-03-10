package main

import (
	quic "github.com/lucas-clemente/quic-go"
	"context"
	"crypto/tls"
	"fmt"
	"strings"
	"strconv"
	"bytes"

	"crypto/rand"
	"crypto/rsa"
	"crypto/x509"
	"encoding/pem"
	"math/big"
)

var (
	addr string = "54.152.23.18:1337"
	buff []byte
)

type stream struct {
	quic.Stream
}

func (str stream) W(msg string) error {
	msg += "\n"
	n, err := str.Write([]byte(msg))

	if err != nil {
		return err
	}

	if len(msg) != n {
		return fmt.Errorf("Entire message not written! %d != %d\n", len(msg), n)
	}

	return nil
}

func extract(buff []byte, n int) string {
	return strings.TrimSpace(string(buff[:n]))
}

func check(e error) {
	if e != nil {
		panic(e)
	}
}

func generateTLSConfig() *tls.Config {
	key, err := rsa.GenerateKey(rand.Reader, 1024)
	if err != nil {
		panic(err)
	}
	template := x509.Certificate{SerialNumber: big.NewInt(1)}
	certDER, err := x509.CreateCertificate(rand.Reader, &template, &template, &key.PublicKey, key)
	if err != nil {
		panic(err)
	}
	keyPEM := pem.EncodeToMemory(&pem.Block{Type: "RSA PRIVATE KEY", Bytes: x509.MarshalPKCS1PrivateKey(key)})
	certPEM := pem.EncodeToMemory(&pem.Block{Type: "CERTIFICATE", Bytes: certDER})

	tlsCert, err := tls.X509KeyPair(certPEM, keyPEM)
	if err != nil {
		panic(err)
	}
	return &tls.Config{
		Certificates: []tls.Certificate{tlsCert},
		NextProtos:   []string{"quic-echo-example"},
	}
}

func main() {
	tlsConf := &tls.Config{
		InsecureSkipVerify: true,
		NextProtos:         []string{"quic-echo-example"},
	}
	session, err := quic.DialAddr(addr, tlsConf, nil)
	check(err)

	rawStr, err := session.OpenStreamSync(context.Background())
	check(err)

	str := stream{rawStr}
	err = str.W("Hello")
	check(err)

	total := 0
	for total < 248 {
		buff = make([]byte, 1)
		n, err := str.Read(buff)
		check(err)
		total += n
	}

	for i := 0; i < 1000; i++ {
		buff = make([]byte, 1024)
		n, err := str.Read(buff)
		check(err)

		numS := extract(buff, n)
		num, err := strconv.Atoi(numS)
		check(err)
		err = str.W(fmt.Sprintf("0x%x", num))
		check(err)
	}

	for {
		buff = make([]byte, 1)
		_, err := str.Read(buff)
		check(err)

		if bytes.Equal(buff, []byte(")")) {
			break
		}
	}

	listener, err := quic.ListenAddr("0.0.0.0:6969", generateTLSConfig(), nil)
	check(err)
	sessS, err := listener.Accept(context.Background())
	check(err)
	rawStrS, err := sessS.AcceptStream(context.Background())
	check(err)

	strS := stream{rawStrS}
	for {
		buff = make([]byte, 1)
		_, err := strS.Read(buff)
		check(err)

		if bytes.Equal(buff, []byte("!")) {
			_, err = strS.Read(buff)
			check(err)
			break
		}
	}

	for i := 0; i < 1000; i++ {
		buff = make([]byte, 32)
		n, err := strS.Read(buff)
		check(err)
		request := strings.Split(string(buff[:n]), "+")
		num1, err := strconv.Atoi(strings.TrimSpace(request[0]))
		check(err)
		num2, err := strconv.Atoi(strings.TrimSpace(request[1]))
		check(err)
		sum := num1 + num2
		resp := strconv.Itoa(sum)
		err = strS.W(resp)
		check(err)
	}

	flag := ""
	for {
		buff = make([]byte, 1)
		_, err := strS.Read(buff)
		check(err)
		flag += string(buff)

		if bytes.Equal(buff, []byte("}")) {
			break
		}
	}
	fmt.Println(flag)
}

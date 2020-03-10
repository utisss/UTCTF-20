package main

import (
	"fmt"
	"io/ioutil"
	mrand "math/rand"
	"strconv"
	"strings"
	"time"

	"context"
	"crypto/rand"
	"crypto/rsa"
	"crypto/tls"
	"crypto/x509"
	"encoding/pem"
	quic "github.com/lucas-clemente/quic-go"
	"math/big"
)

var flag string

type stream struct {
	quic.Stream
}

func check(err error) {
	if err != nil {
		panic(err)
	}
}

func extract(buff []byte, n int) string {
	return strings.TrimSpace(string(buff[:n]))
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

func main() {
	dat, err := ioutil.ReadFile("flag.txt")
	check(err)
	flag = string(dat)

	fmt.Println("Running Server")
	listener, err := quic.ListenAddr("0.0.0.0:1337", generateTLSConfig(), nil)
	check(err)

	for {
		sess, err := listener.Accept(context.Background())
		if err != nil {
			fmt.Println("Failed to accept client")
			continue
		}

		go connection(sess)
	}
}

func connection(sess quic.Session) {
	rawStr, err := sess.AcceptStream(context.Background())
	if err != nil {
		fmt.Println("Client failed: %e\n", err.Error())
		return
	}
	str := stream{rawStr}

	// Run Server section
	err = server(str)
	if err != nil {
		fmt.Printf("Client failed: %e\n", err.Error())
		return
	}

	time.Sleep(5 * time.Second)

	// Run Client section
	tlsConf := &tls.Config{
		InsecureSkipVerify: true,
		NextProtos:         []string{"quic-echo-example"},
	}
	remote := strings.Split(sess.RemoteAddr().String(), ":")
	addr := fmt.Sprintf("%s:%d", remote[0], 6969)
	session, err := quic.DialAddr(addr, tlsConf, nil)
	if err != nil {
		fmt.Printf("Client failed: %e\n", err.Error())
		return
	}

	rawStr, err = session.OpenStreamSync(context.Background())
	defer rawStr.Close()
	if err != nil {
		fmt.Printf("Client failed: %e\n", err.Error())
		return
	}

	err = client(stream{rawStr})
	if err != nil {
		fmt.Printf("Client failed: %e\n", err.Error())
		return
	}
}

func server(str stream) error {
	buff := make([]byte, 1024)
	n, err := str.Read(buff)
	if err != nil {
		return err
	}
	intro := extract(buff, n)

	if intro != "Hello" {
		err = str.W("Maybe you should start with Hello...")
		if err != nil {
			return err
		}
		return fmt.Errorf("Client didn't say hi :( they said %d-'%v'\n", intro)
	}

	// Prolouge
	err = str.W("Welcome to the super QUICk Server!")
	if err != nil {
		return err
	}
	err = str.W("You might've thought getting the flag would be easy, but it's gonna take a bit more. :D")
	if err != nil {
		return err
	}

	err = str.W("")
	if err != nil {
		return err
	}

	// Random Echo Hex
	err = str.W("I need some help with my Computer Architecture class, could you give me these numbers back in hex?")
	if err != nil {
		return err
	}
	err = str.W("Quickly, of course... :)")
	if err != nil {
		return err
	}

	var r int
	var in string
	buf := make([]byte, 1024)
	for i := 0; i < 1000; i++ {
		r = mrand.Intn(1000000)
		err = str.W(fmt.Sprintf("%d", r))
		if err != nil {
			return err
		}

		str.SetReadDeadline(time.Now().Add(5 * time.Second))
		n, err := str.Read(buf)
		if err != nil {
			return err
		}

		in = strings.TrimSpace(string(buf[:n]))

		x, err := strconv.ParseInt(in, 0, 0)
		if err != nil {
			return err
		}

		if int(x) != r {
			return fmt.Errorf("%v != %v\n", int(x), r)
		}
	}

	err = str.W("Nice job, let's keep going...")
	if err != nil {
		return err
	}

	err = str.W("Can I dial you later? I'll try 6969 ;)")
	if err != nil {
		return err
	}

	time.Sleep(1 * time.Second)
	str.Close()
	return nil
}

func client(str stream) error {
	err := str.W("Hey... you up?")
	if err != nil {
		return err
	}

	err = str.W("Math time!")
	if err != nil {
		return err
	}

	buf := make([]byte, 1024)
	var a, b, sum int
	var in string
	for i := 0; i < 1000; i++ {
		a, b = mrand.Intn(1000000), mrand.Intn(1000000)
		sum = a + b

		request := fmt.Sprintf("%d + %d", a, b)
		err = str.W(request)
		if err != nil {
			return err
		}

		str.SetReadDeadline(time.Now().Add(5 * time.Second))
		n, err := str.Read(buf)
		if err != nil {
			return err
		}

		in = strings.TrimSpace(string(buf[:n]))

		x, err := strconv.ParseInt(in, 0, 0)
		if err != nil {
			return err
		}

		if int(x) != sum {
			return fmt.Errorf("%v + %v != %v\n", a, b, int(x))
		}
	}

	err = str.W(fmt.Sprintf("Great Job!"))
	if err != nil {
		return err
	}

	err = str.W(flag)
	if err != nil {
		return err
	}
	fmt.Println("Flag Delivered")

	return nil
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

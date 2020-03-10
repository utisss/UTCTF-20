const puppeteer = require('puppeteer');
const { Client } = require('pg');
const client = new Client();
const flag = 'utflag{95debad95cfb106081f33ceadc36bf9c}';

function sleep(ms) {
	return new Promise(resolve => setTimeout(resolve, ms));
}

async function monitorChat(chat_id, browser) {
	const homeUrl = 'http://127.0.0.1:8080/';
	const chatUrl = 'http://127.0.0.1:8080/chatt';
	const page = await browser.newPage();
	await page.goto(homeUrl);
	const cookies = [{
		'name': 'chat_id',
		'value': chat_id
	}, {
		'name': 'secret',
		'value': flag
	}];
	await page.setCookie(...cookies);
	await page.goto(chatUrl, {waitUntil: 'networkidle2'});
	await sleep(1000);
	await page.close();
	return;
}

(async () => {
	const client = new Client({
		host: process.env.DB_HOST,
		user: 'chattwbratt',
		database: 'chattwbratt',
		password: process.env.DB_PASS
	});
	await client.connect();
	const chat_ids = await client.query('SELECT chat_id FROM chat_ids WHERE last_used > current_timestamp - interval \'2 minutes\'');
	await client.end();
	const browser = await puppeteer.launch({headless: true, args: ['--disable-web-security', '--disable-xss-auditor', '--no-sandbox']});
	for (index = 0; index < chat_ids.rows.length; index++) {
		await monitorChat(chat_ids.rows[index].chat_id, browser);
	}
	
	await browser.close();
	return;
})();


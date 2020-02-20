const faker = require('faker/locale/en_US');

const newUserName = `aoutestuser${Math.floor(Math.random() * 1000)}${Math.floor(Date.now() / 1000)}`;

const inputFieldsValues = [{
  label: 'New Username',
  value: newUserName
}, {
  label: 'First Name',
  value: 'tester1'
}, {
  label: 'Last Name',
  value: 'Puppeteerdriver'
}, {
  label: 'Email Address',
  value: process.env.CONTACT_EMAIL
}, {
  label: 'Street Address 1',
  value: faker.address.streetName()
}, {
  label: 'City',
  value: faker.address.city()
}, {
  label: 'State',
  value: faker.address.stateAbbr()
}, {
  label: 'Zip Code',
  value: faker.address.zipCode()
}, {
  label: 'Country',
  value: 'U.S.A'
}, {
  label: 'Paste Professional URL here',
  value: faker.internet.url()
}
];

const institutionAffiliationValues = {
  EARLY_CAREER_TENURE_TRACK_RESEARCHER: "Early career tenure-track researcher",
  UNDERGRADUATE_STUDENT: "Undergraduate (Bachelor level) student",
};

module.exports = {
  institutionAffiliationValues,
  inputFieldsValues
};

# coding: utf-8

"""
    AllOfUs Workbench API

    The API for the AllOfUs workbench.

    OpenAPI spec version: 0.1.0
    
    Generated by: https://github.com/swagger-api/swagger-codegen.git
"""


from pprint import pformat
from six import iteritems
import re


class Profile(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """


    """
    Attributes:
      swagger_types (dict): The key is attribute name
                            and the value is attribute type.
      attribute_map (dict): The key is attribute name
                            and the value is json key in definition.
    """
    swagger_types = {
        'username': 'str',
        'contact_email': 'str',
        'enabled_in_fire_cloud': 'bool',
        'free_tier_billing_project_name': 'str',
        'data_access_level': 'DataAccessLevel',
        'given_name': 'str',
        'family_name': 'str',
        'phone_number': 'str',
        'authorities': 'list[Authority]'
    }

    attribute_map = {
        'username': 'username',
        'contact_email': 'contactEmail',
        'enabled_in_fire_cloud': 'enabledInFireCloud',
        'free_tier_billing_project_name': 'freeTierBillingProjectName',
        'data_access_level': 'dataAccessLevel',
        'given_name': 'givenName',
        'family_name': 'familyName',
        'phone_number': 'phoneNumber',
        'authorities': 'authorities'
    }

    def __init__(self, username=None, contact_email=None, enabled_in_fire_cloud=None, free_tier_billing_project_name=None, data_access_level=None, given_name=None, family_name=None, phone_number=None, authorities=None):
        """
        Profile - a model defined in Swagger
        """

        self._username = None
        self._contact_email = None
        self._enabled_in_fire_cloud = None
        self._free_tier_billing_project_name = None
        self._data_access_level = None
        self._given_name = None
        self._family_name = None
        self._phone_number = None
        self._authorities = None
        self.discriminator = None

        self.username = username
        if contact_email is not None:
          self.contact_email = contact_email
        self.enabled_in_fire_cloud = enabled_in_fire_cloud
        if free_tier_billing_project_name is not None:
          self.free_tier_billing_project_name = free_tier_billing_project_name
        self.data_access_level = data_access_level
        if given_name is not None:
          self.given_name = given_name
        if family_name is not None:
          self.family_name = family_name
        if phone_number is not None:
          self.phone_number = phone_number
        if authorities is not None:
          self.authorities = authorities

    @property
    def username(self):
        """
        Gets the username of this Profile.
        researchallofus username

        :return: The username of this Profile.
        :rtype: str
        """
        return self._username

    @username.setter
    def username(self, username):
        """
        Sets the username of this Profile.
        researchallofus username

        :param username: The username of this Profile.
        :type: str
        """
        if username is None:
            raise ValueError("Invalid value for `username`, must not be `None`")

        self._username = username

    @property
    def contact_email(self):
        """
        Gets the contact_email of this Profile.
        email address that can be used to contact the user

        :return: The contact_email of this Profile.
        :rtype: str
        """
        return self._contact_email

    @contact_email.setter
    def contact_email(self, contact_email):
        """
        Sets the contact_email of this Profile.
        email address that can be used to contact the user

        :param contact_email: The contact_email of this Profile.
        :type: str
        """

        self._contact_email = contact_email

    @property
    def enabled_in_fire_cloud(self):
        """
        Gets the enabled_in_fire_cloud of this Profile.
        true if the user is enabled in FireCloud, false if they are not

        :return: The enabled_in_fire_cloud of this Profile.
        :rtype: bool
        """
        return self._enabled_in_fire_cloud

    @enabled_in_fire_cloud.setter
    def enabled_in_fire_cloud(self, enabled_in_fire_cloud):
        """
        Sets the enabled_in_fire_cloud of this Profile.
        true if the user is enabled in FireCloud, false if they are not

        :param enabled_in_fire_cloud: The enabled_in_fire_cloud of this Profile.
        :type: bool
        """
        if enabled_in_fire_cloud is None:
            raise ValueError("Invalid value for `enabled_in_fire_cloud`, must not be `None`")

        self._enabled_in_fire_cloud = enabled_in_fire_cloud

    @property
    def free_tier_billing_project_name(self):
        """
        Gets the free_tier_billing_project_name of this Profile.
        name of the AllOfUs free tier billing project created for this user

        :return: The free_tier_billing_project_name of this Profile.
        :rtype: str
        """
        return self._free_tier_billing_project_name

    @free_tier_billing_project_name.setter
    def free_tier_billing_project_name(self, free_tier_billing_project_name):
        """
        Sets the free_tier_billing_project_name of this Profile.
        name of the AllOfUs free tier billing project created for this user

        :param free_tier_billing_project_name: The free_tier_billing_project_name of this Profile.
        :type: str
        """

        self._free_tier_billing_project_name = free_tier_billing_project_name

    @property
    def data_access_level(self):
        """
        Gets the data_access_level of this Profile.
        what level of data access the user has

        :return: The data_access_level of this Profile.
        :rtype: DataAccessLevel
        """
        return self._data_access_level

    @data_access_level.setter
    def data_access_level(self, data_access_level):
        """
        Sets the data_access_level of this Profile.
        what level of data access the user has

        :param data_access_level: The data_access_level of this Profile.
        :type: DataAccessLevel
        """
        if data_access_level is None:
            raise ValueError("Invalid value for `data_access_level`, must not be `None`")

        self._data_access_level = data_access_level

    @property
    def given_name(self):
        """
        Gets the given_name of this Profile.
        the user's given name (e.g. Alice)

        :return: The given_name of this Profile.
        :rtype: str
        """
        return self._given_name

    @given_name.setter
    def given_name(self, given_name):
        """
        Sets the given_name of this Profile.
        the user's given name (e.g. Alice)

        :param given_name: The given_name of this Profile.
        :type: str
        """

        self._given_name = given_name

    @property
    def family_name(self):
        """
        Gets the family_name of this Profile.
        the user's family  name (e.g. Jones)

        :return: The family_name of this Profile.
        :rtype: str
        """
        return self._family_name

    @family_name.setter
    def family_name(self, family_name):
        """
        Sets the family_name of this Profile.
        the user's family  name (e.g. Jones)

        :param family_name: The family_name of this Profile.
        :type: str
        """

        self._family_name = family_name

    @property
    def phone_number(self):
        """
        Gets the phone_number of this Profile.
        the user's phone number

        :return: The phone_number of this Profile.
        :rtype: str
        """
        return self._phone_number

    @phone_number.setter
    def phone_number(self, phone_number):
        """
        Sets the phone_number of this Profile.
        the user's phone number

        :param phone_number: The phone_number of this Profile.
        :type: str
        """

        self._phone_number = phone_number

    @property
    def authorities(self):
        """
        Gets the authorities of this Profile.
        authorities granted to this user

        :return: The authorities of this Profile.
        :rtype: list[Authority]
        """
        return self._authorities

    @authorities.setter
    def authorities(self, authorities):
        """
        Sets the authorities of this Profile.
        authorities granted to this user

        :param authorities: The authorities of this Profile.
        :type: list[Authority]
        """

        self._authorities = authorities

    def to_dict(self):
        """
        Returns the model properties as a dict
        """
        result = {}

        for attr, _ in iteritems(self.swagger_types):
            value = getattr(self, attr)
            if isinstance(value, list):
                result[attr] = list(map(
                    lambda x: x.to_dict() if hasattr(x, "to_dict") else x,
                    value
                ))
            elif hasattr(value, "to_dict"):
                result[attr] = value.to_dict()
            elif isinstance(value, dict):
                result[attr] = dict(map(
                    lambda item: (item[0], item[1].to_dict())
                    if hasattr(item[1], "to_dict") else item,
                    value.items()
                ))
            else:
                result[attr] = value

        return result

    def to_str(self):
        """
        Returns the string representation of the model
        """
        return pformat(self.to_dict())

    def __repr__(self):
        """
        For `print` and `pprint`
        """
        return self.to_str()

    def __eq__(self, other):
        """
        Returns true if both objects are equal
        """
        if not isinstance(other, Profile):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other

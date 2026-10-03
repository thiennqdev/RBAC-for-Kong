import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

const resources = {
  en: {
    translation: {
      "Projects": "Projects",
      "Users": "Users",
      "Roles": "Roles",
      "Logout": "Logout",
      "Create Project": "Create Project",
      "Project Code": "Project Code",
      "Project Name": "Project Name",
      "Settings": "Settings"
    }
  },
  vi: {
    translation: {
      "Projects": "Dự án",
      "Users": "Người dùng",
      "Roles": "Vai trò",
      "Logout": "Đăng xuất",
      "Create Project": "Tạo dự án",
      "Project Code": "Mã dự án",
      "Project Name": "Tên dự án",
      "Settings": "Cài đặt"
    }
  }
};

i18n
  .use(initReactI18next)
  .init({
    resources,
    lng: "en", // default language
    fallbackLng: "en",
    interpolation: {
      escapeValue: false
    }
  });

export default i18n;
